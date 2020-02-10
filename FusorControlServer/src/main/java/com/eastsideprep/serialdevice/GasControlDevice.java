package com.eastsideprep.serialdevice;

public class GasControlDevice extends SerialDevice {

    GasControlDevice(SerialDevice sd) {
        super(sd);
        this.setStatus("{\"device\":\"GAS\"}");// todo: why is this here?
    }

    public boolean setOpen() {
        return this.set("sol_in", true);
    }

    public boolean setClosed() {
        return this.set("sol_in", false);
    }
 
}
