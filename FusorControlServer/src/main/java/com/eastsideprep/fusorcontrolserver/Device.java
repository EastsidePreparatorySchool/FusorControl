package com.eastsideprep.fusorcontrolserver;

public abstract class Device {

 

    public abstract void write(String s);

    public boolean isControl() {
        // by default no. need to override in subclass
        return false;
    }
}
