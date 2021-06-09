package com.eastsideprep.serialdevice;

public class HvRelayControlDevice extends Arduino {
    
    HvRelayControlDevice(Arduino sd) {
        super(sd);
        this.setStatus("{\"device\":\"HV-RELAY\"}");
    }
    
    public boolean on() {
        return this.set("in", true);
    }
    public boolean off() {
        return this.set("in", false);
    }
}
