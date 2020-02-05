package com.eastsideprep.serialdevice;

//
//
public class CoreDevices {

    DeviceManager dm;

    public VariacControlDevice variac;
    public TMPControlDevice tmp;
    public GasControlDevice gas;
    public SerialDevice needle;

    public CoreDevices(DeviceManager dm) {
        this.dm = dm;
    }

    public void refresh() {
        SerialDevice sd;
        sd = dm.get("VARIAC");
        if (sd != null) {
            this.variac = (VariacControlDevice) sd;
        }
        sd = dm.get("GAS");
        if (sd != null) {
            this.gas = (GasControlDevice) sd;
        }
        sd = dm.get("TMP");
        if (sd != null) {
            this.tmp = (TMPControlDevice) sd;
        }
        sd = dm.get("NEEDLEVALVE");
        if (sd != null) {
            this.needle = sd;
        }
    }

    public static boolean isCoreDevice(String name) {
        switch (name) {
            case "VARIAC":
            case "TMP":
            case "GAS":
            case "NEEDLEVALVE":
                return true;
        }
        return false;
    }

    public boolean complete() {
        // we need all of these. if any of them aren't there, return null

        return !(variac == null || gas == null || tmp == null || needle == null);
    }

    public void fakeMissingCoreDevices() {
        System.out.println("Faking missing core devices");
        if (variac == null) {
            variac = new VariacControlDevice(new NullSerialDevice("VARIAC"));
            dm.register(variac);
        }
        if (gas == null) {
            gas = new GasControlDevice(new NullSerialDevice("GAS"));
            dm.register(gas);
        }
        if (tmp == null) {
            tmp = new TMPControlDevice(new NullSerialDevice("TMP"));
            dm.register(tmp);
        }
        if (needle == null) {
            needle = new NullSerialDevice("NEEDLEVALVE");
            dm.register(needle);
        }
    }
}
