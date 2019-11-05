package com.eastsideprep.fusorcontrolserver;

public class VariacControlDevice extends SerialDevice {
    
    VariacControlDevice(SerialDevice sd) {
        super(sd);
    }
    
    public void setVoltage(int v) {
        this.write("volt" + String.format("%03d", v) + "END");
    }
}
