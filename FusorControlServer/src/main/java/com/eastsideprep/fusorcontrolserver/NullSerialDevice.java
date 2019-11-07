package com.eastsideprep.fusorcontrolserver;

import com.fazecast.jSerialComm.SerialPort;

public class NullSerialDevice extends SerialDevice {
    NullSerialDevice(String name) {
        super(null, name);
    }

    NullSerialDevice(SerialPort p, String name) {
        super(p, name);
    }

    @Override
    public void write(String s) {
        //System.out.println("Writing to null serial device: "+this.name+": "+s);
    }

}
