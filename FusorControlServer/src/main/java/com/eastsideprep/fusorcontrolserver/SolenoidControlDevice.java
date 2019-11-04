package com.eastsideprep.fusorcontrolserver;

public class SolenoidControlDevice extends SerialDevice {

    SerialDevice sd;
    
    SolenoidControlDevice(SerialDevice sd) {
       this.sd = sd;
    }

    public void setOpen() {
        this.sd.write("SETsolonEND");
    }

    public void setClosed() {
        this.sd.write("SETsoloffEND");
    }
}
