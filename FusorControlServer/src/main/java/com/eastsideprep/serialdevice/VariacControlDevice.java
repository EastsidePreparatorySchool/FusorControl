package com.eastsideprep.serialdevice;

public class VariacControlDevice extends Arduino {

    VariacControlDevice(Arduino sd) {
        super(sd);
        this.setStatus("{\"device\":\"VARIAC\"}");
    }

    public boolean setVoltage(float v) {
        return this.set("input_volts", v);
    }

    public boolean quickZero() {
        return this.set("input_volts", 0);
    }

    public boolean emergencyZero() {
        return this.set("input_volts", -1);
    }
}
