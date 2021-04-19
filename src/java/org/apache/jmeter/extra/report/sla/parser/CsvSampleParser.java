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
import java.util.Map;

import static java.lang.Boolean.parseBoolean;

/**
 * Parses an JMeter CSV line.
 */
public class CsvSampleParser extends AbstractModelParser {

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss,SSS");

    private static final String TIME_STAMP = "timeStamp";
    private static final String ELAPSED = "elapsed";
    private static final String LABEL = "label";
    private static final String RESPONSE_CODE = "responseCode";
    private static final String RESPONSE_MESSAGE = "responseMessage";
    private static final String SUCCESS = "success";
    private static final String BYTES = "bytes";

    public CsvSampleParser(JMeterReportModel model) {
        super(model);
    }

    public void parse(Map<String, String> parts) {

        final Date timestamp = parseTimestamp(parts.get(TIME_STAMP));
        final int duration = Integer.parseInt(parts.get(ELAPSED));
        final String label = parts.get(LABEL);
        final String resultCode = parts.get(RESPONSE_CODE);
        final String responseMessage = parts.get(RESPONSE_MESSAGE);
        final boolean success = parseBoolean(parts.get(SUCCESS));
        final long bytesReceived = Long.parseLong(parts.getOrDefault(BYTES, "0"));

        final SampleElement sampleElement = new SampleElement();

        sampleElement.setDuration(duration);
        sampleElement.setTimestamp(timestamp);
        sampleElement.setLabel(label);
        sampleElement.setResultCode(resultCode);
        sampleElement.setResponseMessage(responseMessage);
        sampleElement.setSuccess(success);
        sampleElement.setBytesReceived(bytesReceived);

        addElement(sampleElement);
    }

    private Date parseTimestamp(String value) {
        try {
            if (value.contains(":")) {
                return sdf.parse(value);
            } else {
                return new Date(Long.parseLong(value));
            }
        } catch (Exception e) {
            throw new RuntimeException("Parsing the following date failed: " + value, e);
        }
    }
}
