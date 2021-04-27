package com.eastsideprep.serialdevice;

public class TMPControlDevice extends SerialDevice {

    public TMPControlDevice(SerialDevice sd) {
        super(sd);
        this.setStatus("{\"device\":\"TMP\"}");
    }

    public boolean setOn() {
        return this.set("tmp", true);
    }

    public boolean setOff() {
        this.set("reset", true);
        return this.set("tmp", false);
    }
    
    public boolean setLow() {
        return this.set("lowspeed", true);
    }

    public boolean setHigh() {
        return this.set("lowspeed", false);
    }}
