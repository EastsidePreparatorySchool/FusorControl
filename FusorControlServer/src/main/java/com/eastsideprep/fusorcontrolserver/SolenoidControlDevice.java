package com.eastsideprep.fusorcontrolserver;

public class SolenoidControlDevice extends SerialDevice {

    SolenoidControlDevice(SerialDevice sd) {
      super(sd);
    }

    public void setOpen() {
        this.write("SETsolonEND");
    }

    public void setClosed() {
        this.write("SETsoloffEND");
    }
}
