package com.eastsideprep.fusorcontrolserver;

//
// this class is just a collection object for the most important devices. used by webserver.
//
public class CoreDevices {

    public VariacControlDevice variac;
    public TMPControlDevice tmp;
    public SolenoidControlDevice solenoid;

    public static boolean isCoreDevice(String name) {
        switch (name) {
            case "VARIAC":
            case "TMP":
            case "SOLENOID":
                return true;
        }
        return false;
    }

    public static CoreDevices getCoreDevices(DeviceManager dm) {
        CoreDevices cd = new CoreDevices();

        cd.variac = (VariacControlDevice) dm.get("VARIAC");
        cd.solenoid = (SolenoidControlDevice) dm.get("SOLENOID");
        cd.tmp = (TMPControlDevice) dm.get("TMP");

 

        // we need all of these. if any of them aren't there, return null
        if (cd.variac == null || cd.solenoid == null || cd.tmp == null) {
            return null;
        }

        return cd;
    }

    public static void fakeMissingCoreDevices(DeviceManager dm) {
        if (dm.get("VARIAC") == null) {
            dm.register(new VariacControlDevice(new NullSerialDevice("VARIAC")));
        }
        if (dm.get("SOLENOID")  == null) {
            dm.register(new SolenoidControlDevice(new NullSerialDevice("SOLENOID")));
        }
        if (dm.get("TMP") == null) {
            dm.register(new TMPControlDevice(new NullSerialDevice("TMP")));
        }
    }
}
