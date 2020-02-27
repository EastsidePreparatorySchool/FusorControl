/*
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 United States License.
 * For more information go to http://creativecommons.org/licenses/by-nc/3.0/us/
 */
package com.eastsideprep.fusorweblog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.eastsideprep.weblog.WebLog;
import com.eastsideprep.weblog.WebLogEntry;
import com.eastsideprep.weblog.WebLogState;

/**
 *
 * @author gmein
 */
public class FusorWebLogState implements WebLogState {

    private WebLog log;
    public int entries;
    public boolean forUpdates;
    private ArrayList<FusorWebLogEntry> list;

    // what the console uses initially, 
    // and what "copy" uses internally
    public FusorWebLogState() {
        list = new ArrayList<>();
    }

    // this is for clients who want to compact a set of log entries after getting them
    public FusorWebLogState(boolean forUpdates) {
        this.forUpdates = forUpdates; //purpose of the state, whether to record events or just provide current state
    }

    // this is used by the log to hand a new copy to a client
    @Override
    public WebLogState copy() {
        FusorWebLogState copy = new FusorWebLogState();

        // copy the <reset> record
        if (list.size() > 0) {
            copy.addEntry(list.get(0));
        }

        // deep-copy information (don't need to copy items in list, they are read-only from here
        int dropped = 0;
        for (FusorWebLogEntry e : list) {
            // only copy the last 2 min
            if (e.serverTime > System.currentTimeMillis() - 120000) {
                copy.addEntry(e);
            } else {
                dropped++;
            }
        }
        System.out.println("Initial state: "+copy.entries+", "+dropped+" dropped");

        return copy;
    }

    // the log uses this to compact itself into the state
    @Override
    public void addEntry(WebLogEntry e) {
        FusorWebLogEntry tge = null;
        try {
            tge = (FusorWebLogEntry) e;
        } catch (Exception ex) {
            System.err.println("WebLogState: Invalid log entry added to state: " + ex);
            return;
        }
        list.add(tge);
        entries++;
    }

    // log needs this
    @Override
    public int getEntryCount() {
        return entries;
    }

    // this is what the server will use to get entries to feed to the client. 
    @Override
    public ArrayList<WebLogEntry> getCompactedEntries() {
        ArrayList<WebLogEntry> result = new ArrayList<>();
        for (FusorWebLogEntry e : list) {
            result.add(e);
        }
        return result;
    }

    @Override
    public void onDeath() {
    }

    @Override
    public void setLog(WebLog log) {
        this.log = log;
    }

}
