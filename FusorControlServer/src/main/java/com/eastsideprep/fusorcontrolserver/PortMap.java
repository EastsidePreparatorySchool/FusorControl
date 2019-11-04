package com.eastsideprep.fusorcontrolserver;

import com.fazecast.jSerialComm.SerialPort;
import java.util.HashMap;

public class PortMap extends HashMap<SerialPort, SerialDevice> {

    public SerialDevice get(SerialPort p) {
        SerialDevice sd;
        synchronized (this) {
            sd = super.get(p);
        }
        return sd;
    }

    public boolean containsKey(SerialPort p) {
        boolean result;
        synchronized (this) {
            result = super.containsKey(p);
        }
        return result;
    }

    public void put(SerialDevice sd) {
        synchronized (this) {
            super.put(sd.port, sd);
        }
    }

    public void remove(SerialDevice sd) {
        synchronized (this) {
            super.remove(sd.port);
        }
    }
}
