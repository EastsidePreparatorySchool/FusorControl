package com.eastsideprep.fusorcontrolserver;

import com.fazecast.jSerialComm.SerialPort;
import java.util.ArrayList;
import java.util.HashMap;

public class SerialDeviceMap {

    HashMap<String, SerialDevice> nameMap = new HashMap<>();
    HashMap<String, SerialDevice> portMap = new HashMap<>();

    public SerialDevice get(SerialPort p) {
        SerialDevice sd;
        synchronized (this) {
            sd = portMap.get(p.getSystemPortName());
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
            result = portMap.containsKey(p.getSystemPortName());
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
            //System.out.println("SDM:REGISTER:"+sd.name);
            if (sd.port != null) {
                portMap.put(sd.port.getSystemPortName(), sd);
            } else {
                portMap.put("<fake port:"+sd.name+">", sd);
            }
            nameMap.put(sd.name, sd);
        }
    }

    public void remove(SerialDevice sd) {
        synchronized (this) {
            //System.out.println("SDM:REMOVE:"+sd.name);
            if (sd.port != null) {
                portMap.remove(sd.port.getSystemPortName());
            } else {
                portMap.remove("<fake port:"+sd.name+">");
            }
            nameMap.remove(sd.name);
        }
    }

    public ArrayList<String> getNames() {
        ArrayList<String> names;
        synchronized (this) {
            names = new ArrayList(nameMap.keySet());
        }
        return names;
    }
}
