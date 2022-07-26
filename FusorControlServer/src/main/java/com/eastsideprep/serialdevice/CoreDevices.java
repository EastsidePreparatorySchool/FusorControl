package com.eastsideprep.serialdevice;

//
import com.eastsideprep.fusorcontrolserver.WebServer;

//
public class CoreDevices {

    DeviceManager dm;

    public HvRelayControlDevice hvrelay;
    public VariacControlDevice variac;
    public TMPControlDevice tmp;
    public GasControlDevice gas;
    public PressureGaugeDevice pressure;
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
        sd = dm.get("HV-RELAY");
        if (sd != null) {
            this.hvrelay = (HvRelayControlDevice) sd;
        }
        sd = dm.get("GAS");
        if (sd != null) {
            this.gas = (GasControlDevice) sd;
        }
        sd = dm.get("PIRANI");
        if (sd != null) {
            this.pressure = (PressureGaugeDevice) sd;
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
            case "HV-RELAY":
            case "TMP":
            case "GAS":
            case "PIRANI":
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
        if (hvrelay == null) {
            System.out.println("Faking missing core device HV-RELAY");
            hvrelay = new HvRelayControlDevice(new NullSerialDevice("HV-RELAY"));
            dm.register(hvrelay);
        }
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
        if (pressure == null) {
            System.out.println("Faking missing core device PIRANI");
            this.pressure = new PressureGaugeDevice(new NullSerialDevice("PIRANI"));
            dm.register(this.pressure);
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

    float targetPressure = 0;
    float lastPressure = 0;
    Thread pressureTargetThread = null;

    public void pressureTarget(float value) {
        if (this.targetPressure >= 0) {
            if (this.pressureTargetThread == null) {
                this.pressureTargetThread = new Thread(() -> this.pressureTargetLoop());
                this.pressureTargetThread.start();
            }
        } else {
            // shut it down
            try {
                this.pressureTargetThread.interrupt();
                this.pressureTargetThread.join();
                this.pressureTargetThread = null;
            } catch (InterruptedException e) {
            }

        }

    }

    private void pressureTargetLoop() {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(100);
                this.pressureStep();
            } catch (InterruptedException e) {
                this.pressureTargetThread = null;
                return;
            } catch (Exception e) {
                System.out.println("Pressure target loop exception: " + e);
                e.printStackTrace();
            }
        }

    }

    private void pressureStep() {
        float needleCurrent = this.gas.currentNeedlePercent;
        float currentPressure = this.pressure.getPressure();
        float error = currentPressure - this.targetPressure;
        float derivative = currentPressure - this.lastPressure;
        this.lastPressure = currentPressure;  // keep track of last pressure for next derivative

        float cP = -0.1f; // proportional constant
        float cD = 0; // derivative constant

        // calculate step
        float needleStep = cP * error + cD * derivative;

        // limit step size
        needleStep = Math.min(needleStep, 5);
        needleStep = Math.max(needleStep, -5);
        float needleNext = needleCurrent + needleStep;

        // limit absolute needle range
        needleNext = Math.min(needleNext, 30);
        needleNext = Math.max(needleNext, 0);

        if (Math.abs(needleCurrent - needleNext) < 0.05) {
            // too small a difference, get out of here
            return;
        }

        boolean success = this.gas.setNV(needleNext);
        if (!success) {
            System.err.println("Automatic needle/pressure control command failed");
            this.pressureTargetThread.interrupt();
        }
    }
}
