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
package com.github.sgoeschl.jmeter.report.sla.parser;

import com.github.sgoeschl.jmeter.report.sla.JMeterReportModel;
import com.github.sgoeschl.jmeter.report.sla.element.SampleElement;
import com.github.sgoeschl.jmeter.report.sla.stax.ComponentParser;

import javax.xml.stream.XMLStreamReader;
import java.util.Date;
import java.util.Properties;
import java.util.Stack;

/**
 * Parses a JMeter "sample" element (including httpSample).
 */
public class XmlSampleParser extends AbstractModelParser implements ComponentParser {

    private static final int LABEL_LENGTH = 70;
    private static final int RESPONSE_MESSAGE_LENGTH = 255;

    public XmlSampleParser(JMeterReportModel model) {
        super(model);
    }

    /**
     * <pre>
     * &lt;httpSample t="4" lt="4" ts="1301400114405" s="true" lb="Reporting.EnumGroupReport" rc="200" rm="OK" tn="CRM 1-2" dt="text" by="2469"/&gt;
     * </pre>
     * @param streamReader the Stax stream reader
     * @param elementStack the current element stack
     */
    @Override
    public Object startElement(XMLStreamReader streamReader, Stack<Object> elementStack) {

        // parse attributes of HttpSampler
        final Properties attributes = new Properties();
        for (int i = 0; i < streamReader.getAttributeCount(); i++) {
            attributes.put(streamReader.getAttributeName(i).getLocalPart(), streamReader.getAttributeValue(i));
        }

        final int duration = Integer.parseInt(attributes.getProperty("t"));
        final Date timestamp = new Date(Long.parseLong(attributes.getProperty("ts")));
        final String label = trim(attributes.getProperty("lb", ""), LABEL_LENGTH);
        final String resultCode = attributes.getProperty("rc");
        final String responseMessage = trim(attributes.getProperty("rm"), RESPONSE_MESSAGE_LENGTH);
        final boolean success = Boolean.parseBoolean(attributes.getProperty("s"));
        final int bytesReceived = Integer.parseInt(attributes.getProperty("by", "0"));

        final SampleElement sampleElement = new SampleElement();
        sampleElement.setDuration(duration);
        sampleElement.setTimestamp(timestamp);
        sampleElement.setLabel(label);
        sampleElement.setResultCode(resultCode);
        sampleElement.setResponseMessage(responseMessage);
        sampleElement.setSuccess(success);
        sampleElement.setBytesReceived(bytesReceived);

        return sampleElement;
    }

    @Override
    public void endElement(XMLStreamReader streamReader, Stack<Object> elementStack) {
        final SampleElement sampleElement = (SampleElement) elementStack.peek();
        addElement(sampleElement);
    }

    private String trim(String input, int maxLength) {
        if (input == null || input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength - 2) + "..";
    }

}
