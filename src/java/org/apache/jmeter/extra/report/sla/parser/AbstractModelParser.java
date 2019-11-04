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
import org.apache.jmeter.extra.report.sla.element.AssertionResultElement;
import org.apache.jmeter.extra.report.sla.element.SampleElement;

import java.util.Date;

public abstract class AbstractModelParser {

    private final JMeterReportModel model;

    public AbstractModelParser(JMeterReportModel model) {
        this.model = model;
    }

    protected void addElement(SampleElement sampleElement) {

        final String label = sampleElement.getLabel();
        final Date timestamp = sampleElement.getTimestamp();
        final long duration = sampleElement.getDuration();
        final long bytesReceived = sampleElement.getBytesReceived();

        if (sampleElement.isSuccess()) {
            model.addSuccess(label, timestamp, duration, bytesReceived);
        } else {
            final String resultCode;
            final String responseMessage;

            if (!sampleElement.getAssertionResultList().isEmpty()) {
                final AssertionResultElement assertionResult = sampleElement.getAssertionResultList().get(0);
                resultCode = assertionResult.getName();
                responseMessage = assertionResult.getFailureMessage();
            } else {
                resultCode = sampleElement.getResultCode();
                responseMessage = sampleElement.getResponseMessage();
            }

            final String failureResultCode = createFailureResultCode(resultCode, responseMessage);

            model.addFailure(
                    label,
                    timestamp,
                    duration,
                    failureResultCode,
                    null);
        }
    }

    private String createFailureResultCode(String errorCode, String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return errorCode;
        } else {
            return errorCode + " - " + errorMessage;
        }
    }
}
