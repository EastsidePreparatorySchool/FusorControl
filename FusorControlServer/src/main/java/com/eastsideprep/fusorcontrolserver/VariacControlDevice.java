package com.eastsideprep.fusorcontrolserver;

public class VariacControlDevice extends SerialDevice {
    
    VariacControlDevice(SerialDevice sd) {
        super(sd);
        this.setStatus("{\"device\":\"VARIAC\"}");
    }
    
    public void setVoltage(int v) {
        this.set("input_volts", v);
    }
}
