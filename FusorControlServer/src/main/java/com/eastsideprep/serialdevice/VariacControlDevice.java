package com.eastsideprep.serialdevice;

public class VariacControlDevice extends SerialDevice {
    
    VariacControlDevice(SerialDevice sd) {
        super(sd);
        this.setStatus("{\"device\":\"VARIAC\"}");
    }
    
    public boolean setVoltage(int v, CoreDevices cd) {
        if (v == 0 || v == -1) {
            cd.hvrelay.off();
        } else {
            cd.hvrelay.on();
        }
        return this.set("input_volts", v);
    }
}
