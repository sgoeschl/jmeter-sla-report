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

import com.jamonapi.*;

import java.util.Date;

/**
 * Integrates JAMON with JMeter.
 */
public class JMeterReportModel {

    /** the units being used */
    public static final String UNIT_MS = "ms.";
    public static final String UNIT_EXCEPTION = "Exception";
    public static final String UNIT_JMETER_ERRORS="JMeter Errors";

    static {
        MonitorFactory.setRangeDefault(UNIT_MS, JMeterReportModel.createMSHolder());
    }

    /**
     * @return a customized range holder for measuring the execution time for services.
     */
    private static RangeHolder createMSHolder() {
        RangeHolder result = new RangeHolder("<");
        result.add("0_10ms", 10);
        result.add("10_20ms", 20);
        result.add("20_40ms", 40);
        result.add("40_80ms", 80);
        result.add("80_160ms", 160);
        result.add("160_320ms", 320);
        result.add("320_640ms", 640);
        result.add("640_1280ms", 1280);
        result.add("1280_2560ms", 2560);
        result.add("2560_5120ms", 5120);
        result.add("5120_10240ms", 10240);
        result.add("10240_20480ms", 20480);
        result.addLastHeader("20480ms_");
        // note last range is always called lastRange and is added automatically
        return result;
    }

    public static void addSuccess(String label, Date timestamp, long duration) {
        Monitor mon = MonitorFactory.getMonitor(label, UNIT_MS).start();
        if(mon.getFirstAccess().getTime()  == 0) {
            mon.setFirstAccess(timestamp);
        }
        mon.add(duration);
        mon.stop();
        mon.setLastAccess(timestamp);
    }

    public static void addFailure(String label, Date timestamp, long duration, String resultCode, String resultMessage) {

        addSuccess(label, timestamp, duration );
        Monitor mon = MonitorFactory.getMonitor(label + " - " + resultCode + " - " + resultMessage, UNIT_JMETER_ERRORS);
        if(mon.getFirstAccess().getTime()  == 0) {
            mon.setFirstAccess(timestamp);
        }
        mon.add(duration);
        mon.stop();
        mon.setLastAccess(timestamp);

        Object[] details = new Object[] {label, resultCode, resultMessage};
        MonKeyImp monKey = new MonKeyImp(label, details, UNIT_EXCEPTION);
        MonitorFactory.add(monKey, 1);
        // System.out.println(timestamp + ":" + label + ":" + resultCode + ":" + resultMessage);
    }

}
