package org.apache.jmeter.extra.report.sla.utils;

import java.util.ArrayList;

public class BoundedList<E> extends ArrayList<E> {
    private int limit;

    public BoundedList(int limit) {
        this.limit = limit;
    }

    @Override
    public synchronized boolean add(E e) {
        if (size() < limit) {
            return super.add(e);
        }
        return false;
    }
}
