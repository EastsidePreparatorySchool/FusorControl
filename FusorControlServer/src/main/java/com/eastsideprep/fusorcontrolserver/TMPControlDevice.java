package com.eastsideprep.fusorcontrolserver;

public class TMPControlDevice extends SerialDevice {

    public TMPControlDevice(SerialDevice sd) {
        super(sd);
    }

    public void setOn() {
        this.set("tmp", "on");
    }

    public void setOff() {
        this.set("tmp", "off");
    }
}
