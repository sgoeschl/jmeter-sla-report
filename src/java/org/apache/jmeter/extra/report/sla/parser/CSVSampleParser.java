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
package org.apache.jmeter.extra.report.sla.parser;

import org.apache.jmeter.extra.report.sla.JMeterReportModel;
import org.apache.jmeter.extra.report.sla.element.SampleElement;

import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.Boolean.parseBoolean;

/**
 * Parses an JMeter CSV line.
 */
public class CSVSampleParser extends AbstractModelParser {

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss,SSS");

    // time, exectime msec, sampler nanme, http status, http msg, thread name, ?, success, ?, ? 
    private static final int TIMESTAMP_INDEX = 0;
    private static final int DURATION_INDEX = 1;
    private static final int LABEL_INDEX = 2;
    private static final int HTTP_CODE_INDEX = 3;
    private static final int HTTP_MSG_INDEX = 4;
    private static final int STATUS_INDEX = 7;

    public CSVSampleParser(JMeterReportModel model) {
        super(model);
    }

    /**
     * 1329852757203,128,Initialize,200,OK,Setup 1-1,text,true,0,0
     * 2019/02/26 11:55:33,538;3020;ENROLLMENT;200;;GeorgeGo_Enrollment 1-3;text;true;1384;9;9;https://georgego-perf.slsp.sk/testclient/test/locker/umbrella/skperf/enrollAndSign;2984;sk06prf04v.dctest.slsp.sk;0;110095963;null;null;null;null;172.16.8.156;null
     *
     * @param line the line in the CSV
     */
    public void parse(String line) {
        final String[] parts = line.split(",");

        final int duration = Integer.parseInt(parts[DURATION_INDEX]);
        final Date timestamp = parseTimestamp(parts[TIMESTAMP_INDEX]);
        final String label = parts[LABEL_INDEX];
        final String resultCode = parts[HTTP_CODE_INDEX];
        final String responseMessage = parts[HTTP_MSG_INDEX];
        final boolean success = parseBoolean(parts[STATUS_INDEX]);

        final SampleElement sampleElement = new SampleElement();

        sampleElement.setDuration(duration);
        sampleElement.setTimestamp(timestamp);
        sampleElement.setLabel(label);
        sampleElement.setResultCode(resultCode);
        sampleElement.setResponseMessage(responseMessage);
        sampleElement.setSuccess(success);

        addElement(sampleElement);
    }

    private Date parseTimestamp(String value) {
        try {
            if(value.contains(":")) {
                return sdf.parse(value);
            }
            else {
                return new Date(Long.parseLong(value));
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Parsing the following date failed: " + value, e);
        }
    }

}
