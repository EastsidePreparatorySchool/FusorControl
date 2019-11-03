package com.eastsideprep.fusorcontrolserver;

public class TMPControlDevice extends SerialDevice {
    
    TMPControlDevice(SerialDevice sd) {
        clone(sd);
    }
    
    public void setOn() {
        write("SETtmponEND");
    }

    public void setOff() {
        write("SETtmpoffEND");
    }
}
