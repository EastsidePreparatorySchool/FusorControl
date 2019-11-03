package com.eastsideprep.fusorcontrolserver;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author pmaclean
 */
public class DeviceManager {

    private final ArrayList<Device> devices = new ArrayList<>();
    private final HashMap<String, Device> deviceMap = new HashMap<>();

    public void writeAll(String arg) {
        for (Device d : devices) {
            try {
                if (d != null) {
                    d.write(arg);
                }
            } catch (Exception ex) {
                System.out.println("DeviceMgr writeAll exception: " + ex + " " + "in device " + d);
            }
        }
    }

    public void writeControl(String arg) {
        for (Device d : devices) {
            if (!d.isControl()) {
                continue;
            }
            try {
                d.write(arg);
            } catch (Exception ex) {
                System.out.println("DeviceMgr writeControl exception: " + ex + " " + "in " + d);
            }
        }
    }

    public void test() {
        writeAll("TESmemeEND");
    }

    public void init() {
        SerialDevice.init(this);
    }

    public void registerDevice(String name, Device device) {
        deviceMap.put(name, device);
    }

    public Device getDevice(String name) {
        return deviceMap.get(name);
    }

    public CoreDevices getCoreDevices() {
        CoreDevices cd = new CoreDevices();
        cd.variac = (VariacControlDevice) getDevice("VARIAC");
        cd.solenoid = (SolenoidControlDevice) getDevice("SOLENOID");
        cd.tmp = (TMPControlDevice) getDevice("TMP");

        return cd;
    }

    public void updateAllStatus() {
        writeAll("GETstatusEND");
    }
}
