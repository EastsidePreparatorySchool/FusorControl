/*
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 United States License.
 * For more information go to http://creativecommons.org/licenses/by-nc/3.0/us/
 */
package com.eastsideprep.weblog;

import java.util.ArrayList;

public interface WebLogState {

    WebLogState copy();

    void addEntry(WebLogEntry ge);

    int getEntryCount();

    ArrayList<WebLogEntry> getCompactedEntries();

    void onDeath();

    void setLog(WebLog log);
}
