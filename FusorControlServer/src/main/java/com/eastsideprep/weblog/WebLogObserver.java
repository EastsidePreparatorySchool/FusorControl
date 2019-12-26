/*
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 United States License.
 * For more information go to http://creativecommons.org/licenses/by-nc/3.0/us/
 */
package com.eastsideprep.weblog;

import java.util.ArrayList;

/**
 *
 * @author gmein
 */
public class WebLogObserver {

    private final WebLog log;
    WebLogState myState;
    int maxRead;
    boolean stateServed = false;
    boolean stale = false;
    long timeLastObserved;
    public String client;

    public WebLogObserver(WebLog l, String client) {
        log = l;
        maxRead = -1;
        timeLastObserved = System.currentTimeMillis();
        this.client = client;
    }
    
    public int getMaxRead() {
        return maxRead;
    }

    public WebLogState getInitialState() {
        timeLastObserved = System.currentTimeMillis();

        return myState;
    }

    public ArrayList<WebLogEntry> getNewItems() {
        if (isStale()) {
            return null;
        }

        timeLastObserved = System.currentTimeMillis();
        ArrayList<WebLogEntry> result;
        if (stateServed) {
            result = log.getNewItems(this);
            if (result.size() > 0) {
                //System.out.println("Obs " + this.hashCode() + ": Serving incremental state: " + result.size() + " items");
            }
        } else {
            stateServed = true;
            result = myState.getCompactedEntries();
            //System.out.println("Obs " + this.hashCode() + ": Serving initial state: " + result.size() + " items");
        }
        return result;
    }

    public boolean isStale() {
        if (stale) {
            return true;
        }

        // kill it after 60 seconds of no reading
        if ((System.currentTimeMillis() - timeLastObserved) > 60000) {
            stale = true;
            return true;
        }
        
        return false;
    }
}
