package com.eastsideprep.serialdevice;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;

public class NullSerialDevice extends Arduino {
    
    
    public NullSerialDevice(String name) {
        super(null, name);
        System.out.println("Creating null serial device " + name);
    }

    public NullSerialDevice(SerialPort p, String name) {
        super(p, name);
        System.out.println("Creating null serial device " + name + " for port " + p.getSystemPortName());
    }

    @Override
    public boolean write(String s) {
        //System.out.println("Writing to null serial device: "+this.name+": "+s);
        return true;
    }

    @Override
    void processSerialData(SerialPortEvent e) {
        throw new UnsupportedOperationException("This cannot happen."); 
    }

}
