package com.eastsideprep.fusorcontrolserver;

public class TMPControlDevice extends SerialDevice {
    
    TMPControlDevice(SerialDevice sd) {
        clone(sd);
    }
    
    public void setOn() {
        this.write("SETtmponEND");
    }

    public void setOff() {
        this.write("SETtmpoffEND");
    }
}
