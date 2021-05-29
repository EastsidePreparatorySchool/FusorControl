package com.eastsideprep.serialdevice;

import com.eastsideprep.fusorcontrolserver.DataLogger;
import com.fazecast.jSerialComm.SerialPort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SerialDeviceMap {

    private HashMap<String, SerialDevice> nameMap = new HashMap<>();
    private HashMap<String, SerialDevice> portMap = new HashMap<>();

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
                portMap.put("<fake port:" + sd.name + ">", sd);
            }
            nameMap.put(sd.name, sd);
        }
    }

    public void prunePortList(List<SerialPort> list) {
        HashSet<String> set = new HashSet<>(list.stream().map((sp) -> sp.getSystemPortName()).collect(Collectors.toList()));
        ArrayList<SerialDevice> removals = new ArrayList<>();
        Set<String> ports;
        synchronized (this) {
            ports = this.portMap.keySet();
        }
        for (String port : ports) {
            SerialDevice sd = portMap.get(port);
            if (sd.isValid() || sd.name.equals("<unknown>")) {
                //System.out.println("Examining SD:" + sd.name + "(supposedly " + port + ")");
                if (!set.contains(port)) {
                    System.out.println("not present, removing: " + port + " (" + sd.name + ")");
                    DataLogger.recordSDAdvisory("Port not present, removing: " + port + " (" + sd.name + ")");
                    removals.add(sd);
                } else {
                    // is valid serial device. Arduino?
                    if (sd instanceof Arduino) {
                        if (!sd.command("IDENTIFY")) {
                            System.out.println("not responding, removing: " + port + " (" + sd.name + ")");
                            DataLogger.recordSDAdvisory("Not responding, removing: " + port + " (" + sd.name + ")");
                            removals.add(sd);
                        }
                    }
                }
            }
        }

        for (SerialDevice sd : removals) {
            remove(sd);
        }
    }

    public void remove(SerialDevice sd) {
        synchronized (this) {
            //System.out.println("SDM:REMOVE:"+sd.name);
            if (sd.port != null) {
                portMap.remove(sd.port.getSystemPortName());
            } else {
                portMap.remove("<fake port:" + sd.name + ">");
            }
            nameMap.remove(sd.name);
            try {
                sd.port.closePort();
            } catch (Exception e) {
                System.out.println("Exception trying to close port for sd "+sd.name+", port "+sd.port.getSystemPortName());
            }
        }
    }

    public ArrayList<String> getNames() {
        ArrayList<String> names;
        synchronized (this) {
            names = new ArrayList(nameMap.keySet());
        }
        return names;
    }

    public ArrayList<SerialDevice> getAllDevices() {
        ArrayList<SerialDevice> devices;
        synchronized (this) {
            devices = new ArrayList(nameMap.values());
        }
        return devices;
    }

    public int validDeviceCount() {
        int num;
        synchronized (this) {
            num = (int) nameMap.values().stream().filter((v) -> (v.isValid())).count();
        }
        return num;

    }
}
