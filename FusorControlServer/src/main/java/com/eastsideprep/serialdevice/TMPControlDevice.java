package com.eastsideprep.serialdevice;

public class TMPControlDevice extends Arduino {

    public TMPControlDevice(Arduino sd) {
        super(sd);
        this.setStatus("{\"device\":\"TMP\"}");
    }

    public boolean setOn() {
        return this.set("tmp", true);
    }

    public boolean setOff() {
        boolean result =  this.set("tmp", false);
        this.set("reset", true);
        return result;
    }
    
    public boolean setLow() {
        return this.set("lowspeed", true);
    }

    public boolean setHigh() {
        return this.set("lowspeed", false);
    }}
