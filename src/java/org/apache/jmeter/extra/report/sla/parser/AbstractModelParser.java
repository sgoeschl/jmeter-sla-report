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

public abstract class AbstractModelParser {
    final JMeterReportModel model;

    public AbstractModelParser(JMeterReportModel model) {
        this.model = model;
    }

    protected void addElement(SampleElement sampleElement) {

        if (sampleElement.isSuccess()) {
            model.addSuccess(sampleElement.getLabel(), sampleElement.getTimestamp(), sampleElement.getDuration());
        } else {

            String resultCode = sampleElement.getResultCode();
            String responseMessage = sampleElement.getResponseMessage();

            if (sampleElement.getAssertionResultList().size() > 0) {
                final AssertionResultElement assertionResult = sampleElement.getAssertionResultList().get(0);
                resultCode = assertionResult.getName();
                responseMessage = assertionResult.getFailureMessage();
            }

            model.addFailure(sampleElement.getLabel(), sampleElement.getTimestamp(), sampleElement.getDuration(), resultCode,
                    responseMessage);
        }
    }

}
