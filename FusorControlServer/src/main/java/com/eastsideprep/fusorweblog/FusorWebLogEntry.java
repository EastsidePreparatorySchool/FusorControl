/*
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 United States License.
 * For more information go to http://creativecommons.org/licenses/by-nc/3.0/us/
 */
package com.eastsideprep.fusorweblog;

import com.eastsideprep.weblog.WebLogEntry;

/**
 *
 * @author gmein
 */
public class FusorWebLogEntry extends WebLogEntry {

    public String device;
    public long serverTime;
    public String data;

    public FusorWebLogEntry(String device, long serverTime, String data) {
        this.device = device;
        this.serverTime = serverTime;
        this.data = data;
    }
}
