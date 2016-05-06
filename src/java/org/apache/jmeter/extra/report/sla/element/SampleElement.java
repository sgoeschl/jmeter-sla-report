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
package org.apache.jmeter.extra.report.sla.element;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Models a "sample" element (including httpSample).
 */
public class SampleElement {

    private long duration;
    private Date timestamp;
    private String label;
    private String resultCode;
    private String responseMessage;
    private boolean success;
    private final List<AssertionResultElement> assertionResultList = new ArrayList<>();


    public long getDuration() {
        return duration;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getLabel() {
        return label;
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<AssertionResultElement> getAssertionResultList() {
        return assertionResultList;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
