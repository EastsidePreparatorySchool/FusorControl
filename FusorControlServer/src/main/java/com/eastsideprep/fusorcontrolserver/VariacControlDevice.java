package com.eastsideprep.fusorcontrolserver;

public class VariacControlDevice extends SerialDevice {
    
    VariacControlDevice(SerialDevice sd) {
        clone(sd);
    }
    
    public void setVoltage(int v) {
        this.write("SETvolt" + String.format("%03d", v) + "END");
    }
}
