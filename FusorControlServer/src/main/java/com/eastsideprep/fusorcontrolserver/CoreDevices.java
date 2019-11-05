package com.eastsideprep.fusorcontrolserver;

//
// this class is just a collection object for the most important devices. used by webserver.
//
public class CoreDevices {

    DeviceManager dm;

    public VariacControlDevice variac;
    public TMPControlDevice tmp;
    public SolenoidControlDevice solenoid;

    public CoreDevices(DeviceManager dm) {
        this.dm = dm;

        this.variac = (VariacControlDevice) dm.get("VARIAC");
        this.solenoid = (SolenoidControlDevice) dm.get("SOLENOID");
        this.tmp = (TMPControlDevice) dm.get("TMP");
    }

    public boolean isCoreDevice(String name) {
        switch (name) {
            case "VARIAC":
            case "TMP":
            case "SOLENOID":
                return true;
        }
        return false;
    }

 

    public boolean complete() {
        // we need all of these. if any of them aren't there, return null

        return !(variac == null || solenoid == null || tmp == null);
    }

    public void fakeMissingCoreDevices() {
        if (dm.get("VARIAC") == null) {
            dm.register(new VariacControlDevice(new NullSerialDevice("VARIAC")));
        }
        if (dm.get("SOLENOID") == null) {
            dm.register(new SolenoidControlDevice(new NullSerialDevice("SOLENOID")));
        }
        if (dm.get("TMP") == null) {
            dm.register(new TMPControlDevice(new NullSerialDevice("TMP")));
        }
    }
}
