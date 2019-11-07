package com.eastsideprep.fusorcontrolserver;

public class SolenoidControlDevice extends SerialDevice {

    SolenoidControlDevice(SerialDevice sd) {
        super(sd);
    }

    public void setOpen() {
        this.set("solenoid", "open");
    }

    public void setClosed() {
        this.set("solenoid", "closed");
    }
}
