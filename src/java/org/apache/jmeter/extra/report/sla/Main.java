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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A JMeter XML Report post processor to efficiently process gigabytes of JMeter reports.
 */
public class Main {

    public static void main(String args[]) throws Exception {
        try {
            onMain(args);
        } catch (Exception e) {
            System.err.println("Failed to create JMeter SLA report: " + e.getMessage());
            System.exit(1);
        }
    }

    public static int onMain(String args[]) throws Exception {

        if (args == null || args.length < 1) {
            System.err.println("Expecting at least one command-line argument");
            System.err.println("Usage: java -jar jmeter-sla-report-1.0.5.jar output [sources]*");
            return 1;
        }

        final File reportFile = new File(args[0]);
        final List<File> sourceFiles = getSourceFiles(args);
        final JMeterReportModel reportModel = createReportModel(sourceFiles);
        createReport(reportFile, sourceFiles.get(0).getAbsolutePath(), reportModel);
        return 0;
    }

    private static List<File> getSourceFiles(String args[]) {

        final List<File> sourceFiles = new ArrayList<File>();

        // when no source files are passed we pick up CSV/JTL from the current working directory
        if (args.length == 1) {
            sourceFiles.add(new File("."));
        }

        for (int i = 1; i < args.length; i++) {
            final File sourceFile = new File(args[i]);
            if (!sourceFile.exists()) {
                final String msg = "The following JMeter JTL file was not found : " + sourceFile.getAbsolutePath();
                throw new RuntimeException(msg);
            } else {
                sourceFiles.add(sourceFile);
            }
        }

        return sourceFiles;
    }

    private static JMeterReportModel createReportModel(List<File> sourceFiles) {
        final JMeterReportModel model = new JMeterReportModel();
        final JMeterReportParser parser = new JMeterReportParser(model);
        parser.setSourceFiles(sourceFiles);
        parser.run();
        return model;
    }

    private static void createReportDirectory(File targetFile) {
        final File targetDirectory = targetFile.getParentFile();
        if (targetDirectory != null && !targetDirectory.exists()) {
            if (!targetDirectory.mkdirs()) {
                throw new RuntimeException("Failed to create " + targetDirectory.getAbsolutePath());
            }
        }
    }

    private static void createReport(File reportFile, String reportSource, JMeterReportModel model) throws IOException {
        final Locale locale = Locale.getDefault();
        final int sortColumn = JMeterHtmlReportWriter.DISPLAY_HEADER_FIRSTACCESS_INDEX;
        final String sortOrder = "asc";

        createReportDirectory(reportFile);

        final BufferedWriter out = new BufferedWriter(new FileWriter(reportFile));

        try {
            System.setProperty("jmeter.source.file", reportSource);
            out.write(new JMeterHtmlReportWriter(model, sortColumn, sortOrder, locale).createReport());
        } finally {
            System.setProperty("jmeter.source.file", "");
            out.close();
        }
    }
}
