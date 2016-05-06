package org.apache.jmeter.extra.report.sla;

import com.jamonapi.FactoryEnabled;
import com.jamonapi.MonKey;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorComposite;
import com.jamonapi.MonitorFactoryInterface;
import com.jamonapi.RangeHolder;

public class MonitorProvider {

    private final MonitorFactoryInterface factory;

    public MonitorProvider(String type, RangeHolder rangeHolder) {
        factory = new FactoryEnabled();
        factory.setRangeDefault(type, rangeHolder);
    }

    public Monitor get(String label, String type) {
        return factory.getMonitor(label, type);
    }

    public void add(MonKey key) {
        factory.add(key, 1);
    }

    public MonitorComposite getRoot() {
        return factory.getRootMonitor();
    }
}
