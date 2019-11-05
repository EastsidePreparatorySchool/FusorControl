package com.eastsideprep.fusorcontrolserver;

public class TMPControlDevice extends SerialDevice {
    
    public TMPControlDevice(SerialDevice sd) {
        super(sd);
    }
   
    public void setOn() {
        this.write("SETtmponEND");
    }

    public void setOff() {
        this.write("SETtmpoffEND");
    }
}
