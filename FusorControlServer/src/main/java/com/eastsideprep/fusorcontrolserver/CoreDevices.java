package com.eastsideprep.fusorcontrolserver;

//
// this class is just a collection object for the most important devices. used by webserver.
//
public class CoreDevices {

    public VariacControlDevice variac;
    public TMPControlDevice tmp;
    public SolenoidControlDevice solenoid;

    public static boolean isCoreDevice(String name) {
        switch(name) {
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

        // need to be able to debug this thing away from the Arduinos
        // so make dummy ports if necessary
        if (FusorControlServer.debug) {
            if (cd.variac == null) {
                cd.variac = new VariacControlDevice(new NullSerialDevice("VARIAC"));
                dm.register(cd.variac);
            }
            if (cd.solenoid == null) {
                cd.solenoid = new SolenoidControlDevice(new NullSerialDevice("SOLENOID"));
                dm.register(cd.solenoid);
            }
            if (cd.tmp == null) {
                cd.tmp = new TMPControlDevice(new NullSerialDevice("TMP"));
                dm.register(cd.tmp);
            }
        }

        // we need all of these. if any of them aren't there, return null
        if (cd.variac == null || cd.solenoid == null || cd.tmp == null) {
            return null;
        }

        return cd;
    }

}
