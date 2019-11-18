package com.eastsideprep.fusorcontrolserver;

public class GasControlDevice extends SerialDevice {

    GasControlDevice(SerialDevice sd) {
        super(sd);
        this.setStatus("{\"device\":\"GAS\"}");
    }

    public void setOpen() {
        this.set("solenoid", true);
    }

    public void setClosed() {
        this.set("solenoid", false);
    }
    
    public void setNeedleValve(int n) {
        this.set("needlevalve", n);
    }
}
