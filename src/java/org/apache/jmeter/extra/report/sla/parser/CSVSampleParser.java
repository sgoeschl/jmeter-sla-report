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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Parses an CSV line.
 */
public class CSVSampleParser extends AbstractModelParser {

    private static final String DURATION_HEADER = "elapsed";
    private static final String TIMESTAMP_HEADER = "timeStamp";
    private static final String LABEL_HEADER = "label";
    private static final String HTTP_CODE_HEADER = "responseCode";
    private static final String HTTP_MSG_HEADER = "responseMessage";
    private static final String STATUS_HEADER = "success";

    // time, exectime msec, sampler nanme, http status, http msg, thread name, ?, success, ?, ? 
    private static final int DURATION_INDEX = 1;
    private static final int TIMESTAMP_INDEX = 0;
    private static final int LABEL_INDEX = 2;
    private static final int HTTP_CODE_INDEX = 3;
    private static final int HTTP_MSG_INDEX = 4;
    private static final int STATUS_INDEX = 7;

    private List<String> sampleHeader = new ArrayList<String>();

    public CSVSampleParser(JMeterReportModel model){
    	super(model);
    }

    public void parseHeader(String header) {
        sampleHeader = Arrays.asList(header.split(","));
    }
    
    /**
     * 1329852757203,128,Initialize,200,OK,Setup 1-1,text,true,0,0
     * 
     * @param line the line in the CSV
     */
    public void parse(String line) {
        if (sampleHeader.size() > 0)
            parseWithHeader(line);
        else
            parseWithoutHeader(line);
    }

    protected void parseWithHeader(String line) {
        String[] parts = line.split(",");

        int durationHeaderIndex = sampleHeader.indexOf(DURATION_HEADER);
        int timestampHeaderIndex = sampleHeader.indexOf(TIMESTAMP_HEADER);
        int labelHeaderIndex = sampleHeader.indexOf(LABEL_HEADER);
        int httpCodeHeaderIndex = sampleHeader.indexOf(HTTP_CODE_HEADER);
        int httpMessageHeaderIndex = sampleHeader.indexOf(HTTP_MSG_HEADER);
        int statusHeaderIndex = sampleHeader.indexOf(STATUS_HEADER);

        int duration = Integer.parseInt(parts[durationHeaderIndex]);
        Date timestamp = new Date(Long.parseLong(parts[timestampHeaderIndex]));
        String label = parts[labelHeaderIndex];
        String resultCode = parts[httpCodeHeaderIndex];
        String responseMessage = httpMessageHeaderIndex > -1? parts[httpMessageHeaderIndex] : "N/A";
        boolean success = Boolean.valueOf(parts[statusHeaderIndex]);

        addSampleElement(duration, timestamp, label, resultCode, responseMessage, success);
    }

    protected void parseWithoutHeader(String line) {
        String[] parts = line.split(",");

        int duration = Integer.parseInt(parts[DURATION_INDEX]);
        Date timestamp = new Date(Long.parseLong(parts[TIMESTAMP_INDEX]));
        String label = parts[LABEL_INDEX];
        String resultCode = parts[HTTP_CODE_INDEX];
        String responseMessage = parts[HTTP_MSG_INDEX];
        boolean success = Boolean.valueOf(parts[STATUS_INDEX]);

        addSampleElement(duration, timestamp, label, resultCode, responseMessage, success);
    }

    protected void addSampleElement(int duration, Date timestamp, String label, String resultCode, String responseMessage, boolean success) {
        SampleElement sampleElement = new SampleElement();
        sampleElement.setDuration(duration);
        sampleElement.setTimestamp(timestamp);
        sampleElement.setLabel(label);
        sampleElement.setResultCode(resultCode);
        sampleElement.setResponseMessage(responseMessage);
        sampleElement.setSuccess(success);

        addElement(sampleElement);
    }
}
