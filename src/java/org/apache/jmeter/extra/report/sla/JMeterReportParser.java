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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.jmeter.extra.report.sla.parser.CsvSampleParser;
import org.apache.jmeter.extra.report.sla.parser.XmlAssertionResultParser;
import org.apache.jmeter.extra.report.sla.parser.XmlSampleParser;
import org.apache.jmeter.extra.report.sla.stax.StaxParser;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A JMeter XML Report post processor to efficiently process gigabytes of JMeter reports.
 */
public class JMeterReportParser implements Runnable {

    private List<File> sourceFiles;
    private final JMeterReportModel model;

    public JMeterReportParser(JMeterReportModel model) {
        this.model = model;
    }

    @Override
    public void run() {

        final XMLInputFactory factory = XMLInputFactory.newInstance();

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
                System.out.println("Encountered an exception while processing the XML and stop parsing file : " + e.getClass()
                        .getName());
                break;
            } finally {
                close(fis);
            }
        }
    }

    private void parseInputAsCsv(InputStream is) throws IOException {
        final CsvSampleParser csvSampleParser = new CsvSampleParser(model);
        final Reader reader = new InputStreamReader(is);
        final CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);

        for (CSVRecord csvRecord : csvParser) {
            final Map<String, String> map = csvRecord.toMap();
            csvSampleParser.parse(map);
        }
    }

    private void parseInputAsXml(FileInputStream fis, XMLInputFactory factory) throws XMLStreamException {
        XMLStreamReader xmlStreamReader = null;
        try {
            xmlStreamReader = factory.createXMLStreamReader(fis);

            final StaxParser staxParser = new StaxParser();
            staxParser.registerParser("sample", new XmlSampleParser(model));
            staxParser.registerParser("httpSample", new XmlSampleParser(model));
            staxParser.registerParser("assertionResult", new XmlAssertionResultParser());
            staxParser.parseElement(xmlStreamReader);
        } finally {
            close(xmlStreamReader);
        }
    }

    public List<File> getSourceFiles() {

        final List<File> result = new ArrayList<>();

        for (File sourceFile : sourceFiles) {

            if (sourceFile.isFile()) {
                result.add(sourceFile);
            } else {

                final File[] listedFiles = sourceFile.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        final String lowerName = name.toLowerCase();
                        final boolean isJtl = lowerName.endsWith(".jtl");
                        final boolean isCsv = lowerName.endsWith(".csv");
                        return isJtl || isCsv;
                    }
                });

                if (listedFiles != null) {
                    Collections.addAll(result, listedFiles);
                }
            }
        }

        return result;
    }

    public void setSourceFiles(List<File> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    private void close(XMLStreamReader xmlStreamReader) {
        try {
            if (xmlStreamReader != null) {
                xmlStreamReader.close();
            }
        } catch (Exception e) {
            System.err.println("Closing the XMLStreamReader failed : " + e.getMessage());
        }
    }

    private void close(FileInputStream fis) {
        try {
            if (fis != null) {
                fis.close();
            }
        } catch (Exception e) {
            System.err.println("Closing the FileInputStream failed : " + e.getMessage());
        }
    }
}
