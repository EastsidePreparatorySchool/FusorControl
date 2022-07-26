package com.eastsideprep.serialdevice;

public class GasControlDevice extends Arduino {

    float currentNeedlePercent = 0;

    GasControlDevice(Arduino sd) {
        super(sd);
        this.setStatus("{\"device\":\"GAS\"}");// todo: why is this here?
    }

    public boolean setOpen() {
        return this.set("sol_in", true);
    }

    public boolean setClosed() {
        return this.set("sol_in", false);
    }

    public boolean setNV(float value) {
        if (!this.set("nv_in", value)) {
            return false;
        }
        this.currentNeedlePercent = value;
        return true;
    }
}
