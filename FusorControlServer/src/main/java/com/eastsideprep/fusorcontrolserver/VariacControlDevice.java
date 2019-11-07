package com.eastsideprep.fusorcontrolserver;

public class VariacControlDevice extends SerialDevice {
    
    VariacControlDevice(SerialDevice sd) {
        super(sd);
    }
    
    public void setVoltage(int v) {
        this.set("volts", Integer.toString(v));
    }
}
