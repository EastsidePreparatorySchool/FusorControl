package com.eastsideprep.fusorcontrolserver;

public class GasControlDevice extends SerialDevice {

    GasControlDevice(SerialDevice sd) {
        super(sd);
        this.setStatus("{\"device\":\"GAS\"}");
    }

    public boolean setOpen() {
        return this.set("solenoid", true);
    }

    public boolean setClosed() {
        return this.set("solenoid", false);
    }
 
}
