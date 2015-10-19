/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.apache.jmeter.extra.report.sla;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.jmeter.extra.report.sla.parser.AssertionResultParser;
import org.apache.jmeter.extra.report.sla.parser.SampleParser;
import org.apache.jmeter.extra.report.sla.stax.CsvParser;
import org.apache.jmeter.extra.report.sla.stax.StaxParser;

/**
 * A JMeter XML Report post processor to efficiently process gigabytes of JMeter reports.
 */
public class JMeterReportParser implements Runnable {

    /**
     * the JMeter JTL source files
     */
    private List<File> sourceFiles;
	private final JMeterReportModel model;

	public JMeterReportParser(JMeterReportModel model) {
		this.model = model;
	}

	public void run() {

        XMLInputFactory factory = XMLInputFactory.newInstance();

        for (File sourceFile : getSourceFiles()) {

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(sourceFile);

                if (sourceFile.getName().endsWith(".csv")) {
                    parseInputAsCsv(fis);
                } else {
                    parseInputAsXml(fis, factory);
                }
            } catch (Exception e) {
                System.out.println("Encountered an exception while processing the XML and stop parsing file : " + e.getClass().getName());
                break;
            } finally {
                close(fis);
            }

        }
    }

    private void parseInputAsCsv(FileInputStream fis) throws IOException {
        Reader csvReader;
        csvReader = new InputStreamReader(fis);

        CsvParser parser = new CsvParser(model);
        parser.parseCsv(csvReader);
    }

    private void parseInputAsXml(FileInputStream fis, XMLInputFactory factory) throws XMLStreamException {
        XMLStreamReader xmlStreamReader = null;
        try {
            xmlStreamReader = factory.createXMLStreamReader(fis);

            StaxParser parser = new StaxParser();
            parser.registerParser("sample", new SampleParser(model));
            parser.registerParser("httpSample", new SampleParser(model));
            parser.registerParser("assertionResult", new AssertionResultParser());
            parser.parseElement(xmlStreamReader);
        } finally {
            close(xmlStreamReader);
        }
    }

    public List<File> getSourceFiles() {

        List<File> result = new ArrayList<>();

        for (File sourceFile : sourceFiles) {

            if (sourceFile.isFile()) {
                result.add(sourceFile);
            } else {

                String[] listedFiles = sourceFile.list(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        String lowerName = name.toLowerCase();
                        boolean isJtl = lowerName.endsWith(".jtl");
                        boolean isCsv = lowerName.endsWith(".csv");
                        return isJtl || isCsv;
                    }
                });

                for (String listedFile : listedFiles) {
                    result.add(new File(listedFile));
                }
            }
        }

        return result;
    }

    public void setSourceFiles(List<File> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    private XMLStreamReader close(XMLStreamReader xmlStreamReader) {
        try {
            if (xmlStreamReader != null) {
                xmlStreamReader.close();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Closing the XMLStreamReader failed : " + e.getMessage());
            return null;
        }
    }

    private FileInputStream close(FileInputStream fis) {
        try {
            if (fis != null) {
                fis.close();
            }
            return null;
        } catch (Exception e) {
            System.err.println("Closing the FileInputStream failed : " + e.getMessage());
            return null;
        }
    }
}
