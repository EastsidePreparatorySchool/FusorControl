/*
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 United States License.
 * For more information go to http://creativecommons.org/licenses/by-nc/3.0/us/
 */
package com.eastsideprep.fusorweblog;

import java.util.ArrayList;
import java.util.HashMap;

import com.eastsideprep.weblog.WebLog;
import com.eastsideprep.weblog.WebLogEntry;
import com.eastsideprep.weblog.WebLogState;

public class FusorWebLogState implements WebLogState {

    private WebLog log;
    private int entries;
    public boolean forUpdates;
    private HashMap<String, FusorWebLogEntry> devices;

    // what the console uses initially, 
    // and what "copy" uses internally
    public FusorWebLogState() {
        devices = new HashMap<>();
    }

    // this is for clients who want to compact a set of log entries after getting them
    public FusorWebLogState(boolean forUpdates) {
        this.forUpdates = forUpdates; //purpose of the state, whether to record events or just provide current state
    }

    // this is used by the log to hand a new copy to a client
    @Override
    public WebLogState copy() {
        FusorWebLogState copy = new FusorWebLogState();

        for (FusorWebLogEntry e : devices.values()) {
            FusorWebLogEntry tge = (FusorWebLogEntry) e;
            copy.addEntry(tge);
        }

        copy.entries = this.entries;

        return copy;
    }

    // the log uses this to compact itself into the state
    @Override
    public void addEntry(WebLogEntry e) {
        FusorWebLogEntry tge = (FusorWebLogEntry) e;
        devices.put(tge.device, tge);
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
        for (FusorWebLogEntry e : devices.values()) {
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
