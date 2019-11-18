package com.eastsideprep.fusorcontrolserver;

//
// this class is just a collection object for the most important devices. used by webserver.
//
public class CoreDevices {

    DeviceManager dm;

    public VariacControlDevice variac;
    public TMPControlDevice tmp;
    public GasControlDevice gas;

    public CoreDevices(DeviceManager dm) {
        this.dm = dm;

        this.variac = (VariacControlDevice) dm.get("VARIAC");
        this.gas = (GasControlDevice) dm.get("GAS");
        this.tmp = (TMPControlDevice) dm.get("TMP");
    }

    public boolean isCoreDevice(String name) {
        switch (name) {
            case "VARIAC":
            case "TMP":
            case "GAS":
                return true;
        }
        return false;
    }

 

    public boolean complete() {
        // we need all of these. if any of them aren't there, return null

        return !(variac == null || gas == null || tmp == null);
    }

    public void fakeMissingCoreDevices() {
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
    }
}
