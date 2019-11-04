package com.eastsideprep.fusorcontrolserver;

import com.fazecast.jSerialComm.SerialPort;
import java.util.HashMap;

public class PortMap {

    HashMap<String, SerialDevice> nameMap = new HashMap<>();
    HashMap<SerialPort, SerialDevice> portMap = new HashMap<>();

    public SerialDevice get(SerialPort p) {
        SerialDevice sd;
        synchronized (this) {
            sd = portMap.get(p);
        }
        return sd;
    }

    public SerialDevice get(String name) {
        SerialDevice sd;
        synchronized (this) {
            sd = nameMap.get(name);
        }
        return sd;
    }

     public boolean containsPort(SerialPort p) {
        boolean result;
        synchronized (this) {
            result = portMap.containsKey(p);
        }
        return result;
    }

     public boolean containsName(String name) {
        boolean result;
        synchronized (this) {
            result = nameMap.containsKey(name);
        }
        return result;
    }

     public void put(SerialDevice sd) {
        synchronized (this) {
            portMap.put(sd.port, sd);
            nameMap.put(sd.name, sd);
        }
    }

    public void remove(SerialDevice sd) {
        synchronized (this) {
            portMap.remove(sd.port);
            nameMap.remove(sd.name);
        }
    }
}
