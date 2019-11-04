package com.eastsideprep.fusorcontrolserver;

public class TMPControlDevice extends SerialDevice {
    
    SerialDevice sd;
    
    TMPControlDevice(SerialDevice sd) {
        this.sd = sd;
    }
    
    public void setOn() {
        this.sd.write("SETtmponEND");
    }

    public void setOff() {
        this.sd.write("SETtmpoffEND");
    }
}
