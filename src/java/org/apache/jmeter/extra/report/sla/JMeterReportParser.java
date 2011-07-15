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

import org.apache.jmeter.extra.report.sla.parser.AssertionResultParser;
import org.apache.jmeter.extra.report.sla.parser.SampleParser;
import org.apache.jmeter.extra.report.sla.stax.StaxParser;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * A JMeter XML Report post processor to efficiently process gigabytes of JMeter reports.
 */
public class JMeterReportParser implements Runnable {

    /**
     * the JMeter JTL source files
     */
    private List<File> sourceFiles;

    public void run() {

        FileInputStream fis = null;
        XMLStreamReader xmlStreamReader = null;

        XMLInputFactory factory = XMLInputFactory.newInstance();

        for (File sourceFile : getSourceFiles()) {

            try {

                fis = new FileInputStream(sourceFile);
                xmlStreamReader = factory.createXMLStreamReader(fis);

                StaxParser parser = new StaxParser();
                parser.registerParser("sample", new SampleParser());
                parser.registerParser("httpSample", new SampleParser());
                parser.registerParser("assertionResult", new AssertionResultParser());
                parser.parseElement(xmlStreamReader);
            } catch (Exception e) {
                System.out.println("Encountered an exception while processing the XML and stop parsing file : " + e.getClass().getName());
                break;
            } finally {
                fis = close(fis);
                xmlStreamReader = close(xmlStreamReader);
            }

        }
    }

    public List<File> getSourceFiles() {

        List<File> result = new ArrayList<File>();

        for (File sourceFile : sourceFiles) {

            if (sourceFile.isFile()) {
                result.add(sourceFile);
            } else {

                String[] listedFiles = sourceFile.list(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".jtl");
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
