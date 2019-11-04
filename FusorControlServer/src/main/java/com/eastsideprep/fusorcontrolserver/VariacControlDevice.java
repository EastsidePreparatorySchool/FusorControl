package com.eastsideprep.fusorcontrolserver;

public class VariacControlDevice extends SerialDevice {
    
    SerialDevice sd;
    
    VariacControlDevice(SerialDevice sd) {
        this.sd = sd;
    }
    
    public void setVoltage(int v) {
        this.sd.write("SETvolt" + String.format("%03d", v) + "END");
    }
}
