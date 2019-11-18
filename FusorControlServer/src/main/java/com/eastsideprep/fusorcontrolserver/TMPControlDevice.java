package com.eastsideprep.fusorcontrolserver;

public class TMPControlDevice extends SerialDevice {

    public TMPControlDevice(SerialDevice sd) {
        super(sd);
        this.setStatus("{\"device\":\"TMP\"}");
    }

    public void setOn() {
        this.set("tmp", true);
    }

    public void setOff() {
        this.set("tmp", false);
    }
}
