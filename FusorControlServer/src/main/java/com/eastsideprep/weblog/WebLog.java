/*
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 United States License.
 * For more information go to http://creativecommons.org/licenses/by-nc/3.0/us/
 */
package com.eastsideprep.weblog;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WebLog {

    private final ReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock rlock;
    private final Lock wlock;

    private WebLogState state;
    private ArrayList<WebLogEntry> log = new ArrayList<>();
    private final LinkedList<WebLogObserver> observers = new LinkedList<>();

    private int start = 0;
    private int end = 0;
    private int minRead = 0;
    private final int COLLAPSE_THRESHOLD = 1000;
    public static WebLog instance;
    public long baseTime;

    public WebLog(WebLogState state) {
        rlock = rwl.readLock();
        wlock = rwl.writeLock();
        this.state = state;
        state.setLog(this);
        WebLog.instance = this;
    }

    public void clear(WebLogState resetState, WebLogEntry resetEntry) {
        wlock.lock();
        try {
            log = new ArrayList<>();
            start = 0;
            end = 0;
            minRead = 0;
            state = resetState;
            state.setLog(this);

            resetObservers();

            if (resetEntry != null) {
                addLogEntry(resetEntry);
                baseTime = resetEntry.time;
            }
        } finally {
            wlock.unlock();
        }

//        printLogInfo("AE");
    }

    public static void staticAddLogEntry(WebLogEntry item) {
        WebLog.instance.addLogEntry(item);
    }

    public void addLogEntry(WebLogEntry item) {
        wlock.lock();
        try {
            log.add(item);
            end++;
        } finally {
            wlock.unlock();
        }

        removeStaleObservers();
//        printLogInfo("AE");
    }

    public void addLogEntries(List<WebLogEntry> list) {
        wlock.lock();
        try {
            log.addAll(list);
            end += list.size();
        } finally {
            wlock.unlock();
        }
        removeStaleObservers();
//        printLogInfo("AES");
    }

    public WebLogObserver addObserver(String client) {
        WebLogObserver obs = null;
        wlock.lock();
        try {
            removeStaleObservers();
            collapseRead();
            synchronized (observers) {
                obs = new WebLogObserver(this, client);
                obs.myState = getNewLogState();
                obs.maxRead = obs.myState.getEntryCount();
                observers.add(obs);
            }
        } finally {
            wlock.unlock();
        }
        //printLogInfo("AO");
        return obs;
    }

    public int getLogSize() {
        return collapseRead();
    }

    public LinkedList<WebLogObserver> getObservers() {
        synchronized (observers) {
            return new LinkedList<>(observers);
        }
    }

    public void removeObserver(WebLogObserver obs) {
        synchronized (observers) {
            observers.remove(obs);
            updateMinRead();
        }
        //printLogInfo("RO");
        collapseRead();
    }

    public int collapseRead() {
        int result = -1;
        updateMinRead();
        wlock.lock();
        try {
            if (minRead - start >= COLLAPSE_THRESHOLD) {
//            printLogInfo("CR1");
//            System.out.println("Log: compacting");
                // process all read items into state log, 
                for (int i = 0; i < minRead - start; i++) {
                    state.addEntry(log.get(i));
                }

                // take the sublist of unread items, make it the new list, 
                ArrayList<WebLogEntry> newLog = new ArrayList<>();
                newLog.addAll(log.subList(minRead - start, end - start));
                log = newLog;

                // and adjust the "start" offset
                start = minRead;
//            printLogInfo("CR2");
                //System.out.println("Log: compacted");
            }
            result = log.size();

        } finally {
            wlock.unlock();
        }
        return result;
    }

    public ArrayList<WebLogEntry> getNewItems(WebLogObserver obs) {
        ArrayList<WebLogEntry> result = new ArrayList<>();

        rlock.lock();
        int oldMinRead = minRead;
        try {
//            printLogInfo("GNI1", obs);
            int items = end - obs.maxRead;
            if (items > 0) {
                // copy the new items to the result
                try {
                    result.addAll(log.subList(obs.maxRead - start, end - start));
                } catch (Exception e) {
                    System.out.println("Exception in log.getNewItems: "+e.getMessage());
                }

                // update maxRead, and possibly minRead
                // need to lock this, multiple threads might want to do it
                synchronized (observers) {
                    int oldMax = obs.maxRead;
                    obs.maxRead = end;

                    // if we were at minRead we might need to move it
                    if (oldMax == minRead) {
                        updateMinRead();
                    }
                }
//                printLogInfo("GNI2", obs);

            }

        } finally {
            rlock.unlock();
        }

        collapseRead();

        return result;
    }

    public WebLogState getNewLogState() {
        WebLogState result = null;
        wlock.lock();
        try {
            result = state.copy();
        } finally {
            wlock.unlock();
        }
        return result;
    }

    private void updateMinRead() {
        int currentMin = end;
        boolean haveStales = false;

        synchronized (observers) {
            for (WebLogObserver o : observers) {
                if (o.isStale()) {
                    haveStales = true;
                } else if (o.maxRead < currentMin) {
                    currentMin = o.maxRead;
                }
            }
        }
        // record new minimum
        minRead = currentMin;

        // deal with delinquents
        // we do this last because we can't update the collection while iterating
        if (haveStales) {
            synchronized (observers) {
                observers.removeIf((o) -> o.isStale());
            }
        }
    }

    public void onDeath() {
        state.onDeath();
    }

    public void removeStaleObservers() {
        synchronized (observers) {
            observers.removeIf((o) -> o.isStale());
        }
    }

    // todo: we will need to give the client a signal that things have changed
    public void resetObservers() {
        synchronized (observers) {
            for (WebLogObserver obs : observers) {
                obs.maxRead = 0;
            }
        }
    }

    private void printLogInfo(String op) {
        if (end < start || minRead < start || minRead > end) {
            System.out.println("---- log corrupt");
        }
        System.out.println("Log" + op + ": array size:" + log.size() + ", start:" + start + ", end:" + end + ", minRead:" + minRead);
    }

    private void printLogInfo(String op, WebLogObserver obs) {
        printLogInfo(op);
        if (obs.maxRead < start || obs.maxRead > end) {
            System.out.println("---- obs data corrupt");
        }
        System.out.println("  obs:" + Integer.toHexString(obs.hashCode()) + ", maxRead:" + obs.maxRead);
    }
}
