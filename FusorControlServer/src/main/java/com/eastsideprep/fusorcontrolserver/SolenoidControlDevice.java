package com.eastsideprep.fusorcontrolserver;

public class SolenoidControlDevice extends SerialDevice {

    SolenoidControlDevice(SerialDevice sd) {
        clone(sd);
    }

    public void setOpen() {
        write("SETsolonEND");
    }

    public void setClosed() {
        write("SETsoloffEND");
    }
}
