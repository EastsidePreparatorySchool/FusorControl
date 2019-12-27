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
        return this.set("tmp", false);
    }
}
