package com.eastsideprep.serialdevice;

public class VariacControlDevice extends SerialDevice {
    
    VariacControlDevice(SerialDevice sd) {
        super(sd);
        this.setStatus("{\"device\":\"VARIAC\"}");
    }
    
    public boolean setVoltage(int v) {
        return this.set("input_volts", v);
    }
}
