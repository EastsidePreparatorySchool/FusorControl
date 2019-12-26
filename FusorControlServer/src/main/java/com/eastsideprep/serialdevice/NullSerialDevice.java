package com.eastsideprep.serialdevice;

import com.fazecast.jSerialComm.SerialPort;

public class NullSerialDevice extends SerialDevice {
    public NullSerialDevice(String name) {
        super(null, name);
    }

    public NullSerialDevice(SerialPort p, String name) {
        super(p, name);
    }

    @Override
    public boolean write(String s) {
        //System.out.println("Writing to null serial device: "+this.name+": "+s);
        return true;
    }

}
