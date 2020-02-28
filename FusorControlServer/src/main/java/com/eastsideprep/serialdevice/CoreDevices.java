package com.eastsideprep.serialdevice;

//
//
public class CoreDevices {

    DeviceManager dm;

    public VariacControlDevice variac;
    public TMPControlDevice tmp;
    public GasControlDevice gas;
    public SerialDevice rp;

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
        sd = dm.get("RP");
        if (sd != null) {
            this.rp = sd;
        }
    }

    public static boolean isCoreDevice(String name) {
        switch (name) {
            case "VARIAC":
            case "TMP":
            case "GAS":
            case "RP":
                return true;
        }
        return false;
    }

    public boolean complete() {
        // we need all of these. if any of them aren't there, return null

        return !(variac == null || gas == null || tmp == null || rp == null);
    }

    public void fakeMissingCoreDevices() {
        if (variac == null) {
            System.out.println("Faking missing core device VARIAC");
            variac = new VariacControlDevice(new NullSerialDevice("VARIAC"));
            dm.register(variac);
        }
        if (gas == null) {
            System.out.println("Faking missing core device GAS");
            gas = new GasControlDevice(new NullSerialDevice("GAS"));
            dm.register(gas);
        }
        if (tmp == null) {
            System.out.println("Faking missing core device TMP");
            tmp = new TMPControlDevice(new NullSerialDevice("TMP"));
            dm.register(tmp);
        }

        if (rp == null) {
            System.out.println("Faking missing core device RP");
            rp = new TMPControlDevice(new NullSerialDevice("RP"));
            dm.register(rp);
        }

    }
}
