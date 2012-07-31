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
import java.util.ArrayList;
import java.util.List;

/**
 * A JMeter XML Report post processor to efficiently process gigabytes of JMeter reports.
 */
public class Main {

    public static void main(String args[]) throws Exception {

        int sortColumn = 12;
        String sortOrder = "asc";
        File targetFile = new File(args[0]);
        List<File> sourceFiles = new ArrayList<File>();

        for(int i=1; i<args.length; i++) {

            File sourceFile = new File(args[i]);

            if(!sourceFile.exists()) {
                System.err.println("The following JMeter JTL file was not found : " + sourceFile.getAbsolutePath());
                System.exit(1);
            }
            else {
                sourceFiles.add(sourceFile);
            }

        }

        if(args.length == 1) {
            sourceFiles.add(new File("."));
        }

        JMeterReportModel model = new JMeterReportModel();
        // parse the JMeter JTL file and feed JAMON
        JMeterReportParser instance = new JMeterReportParser(model);
        instance.setSourceFiles(sourceFiles);
        instance.run();

        // create the HTML report and write it to disk
        targetFile.getParentFile().mkdirs();
        BufferedWriter out = new BufferedWriter(new FileWriter(targetFile));
        out.write(new JMeterHtmlReportWriter(model,sortColumn, sortOrder).createReport());
        out.close();
    }

}
