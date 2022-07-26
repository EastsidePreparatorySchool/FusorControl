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
        System.out.println("Pressure target: " + value);
        this.targetPressure = value;

        if (this.targetPressure >= 0) {
            System.out.println("initiating pressure control thread");

            if (this.pressureTargetThread == null) {
                this.pressureTargetThread = new Thread(() -> this.pressureTargetLoop());
                this.pressureTargetThread.start();
            }
        } else {
            // shut it down
            if (this.pressureTargetThread != null) {
                System.out.println("shutting down pressure control thread");
                try {
                    this.pressureTargetThread.interrupt();
                    this.pressureTargetThread.join();
                    this.pressureTargetThread = null;
                } catch (InterruptedException e) {
                }
            }
        }

    }

    private void pressureTargetLoop() {
        this.gas.suspendPing();
        this.pressure.suspendPing();
        this.gas.setOpen();

        while (!Thread.interrupted()) {
            try {
                Thread.sleep(100);
                this.pressureStep();
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.out.println("Pressure target loop exception: " + e);
                e.printStackTrace();
            }
        }
        System.out.println("Pressure target loop interrupted");
        this.pressureTargetThread = null;
        this.gas.resumePing();
        this.pressure.resumePing();

    }
    
    public void shutdownThreads() {
        this.pressureTarget(-1.0f);
    }

    private void pressureStep() {
        float currentNeedle = this.gas.currentNeedlePercent;
        float currentPressure = this.pressure.getPressure();
        float error = currentPressure - this.targetPressure;
        float derivative = currentPressure - this.lastPressure;
        this.lastPressure = currentPressure;  // keep track of last pressure for next derivative

        System.out.print("NV:" + currentNeedle + ", p:" + currentPressure + ", e:" + error + ", d:" + derivative);
        if (Math.abs(error) < 0.02) {
            System.out.println("target reached");
            return;
        }

        float cP = -0.03f; // proportional constant
        float cD = 0; // derivative constant

        // calculate step
        float needleStep = cP * error + cD * derivative;
        System.out.print(", step:" + needleStep);

        // limit step size
        needleStep = Math.min(needleStep, 5);
        needleStep = Math.max(needleStep, -5);
        float needleNext = currentNeedle + needleStep;
        System.out.print(", clamped step:" + needleStep);

        // limit absolute needle range
        needleNext = Math.min(needleNext, 20);
        needleNext = Math.max(needleNext, 0);

        System.out.print(", next:" + needleNext);
        if (Math.abs(currentNeedle - needleNext) < 0.005) {
            // too small a difference, get out of here
            System.out.println();
            return;
        }
        System.out.println(", action:" + needleNext);

        boolean success = this.gas.setNV(needleNext);
        if (!success) {
            System.err.println("Automatic needle/pressure control command failed");
        }
    }
}
