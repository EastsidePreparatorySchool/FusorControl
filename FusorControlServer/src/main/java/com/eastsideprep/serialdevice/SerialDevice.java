package com.eastsideprep.serialdevice;

import com.eastsideprep.fusorcontrolserver.FusorControlServer;
import com.fazecast.jSerialComm.*;
import java.io.IOException;
import java.io.OutputStream;

public class SerialDevice {

    public String name;
    public String originalName;
    public String function;
    private OutputStream os;
    SerialPort port;
    private String lastStatus;
    private String currentStatus;
    private boolean autoStatus = false;
    private String confirmation = null;
    private final Object confMonitor = new Object();

    public final static String FUSOR_COMMAND_PREFIX = "CMD[";
    public final static String FUSOR_RESPONSE_PREFIX = "RSP[";
    public final static String FUSOR_POSTFIX = "]END";
    public final static String FUSOR_RESPONSE_POSTFIX = "]END\n";

    public final static String FUSOR_IDENTIFY = "IDENTIFY";
    public final static String FUSOR_STATUS = "STATUS";
    public final static String FUSOR_STATUS_AUTO = "STATUS:AUTO";

    public static String makeCommand(String s) {
        return FUSOR_COMMAND_PREFIX + s + FUSOR_POSTFIX;
    }

    public static String extractResponse(String s) {
        return "";
    }

    SerialDevice() {
    }

    SerialDevice(SerialPort p, String name) {
        this.name = name;
        this.originalName = name;
        this.function = "generic";
        this.port = p;
        this.os = (p != null ? p.getOutputStream() : null);
        this.lastStatus = null;
        this.currentStatus = null;
    }

    SerialDevice(SerialDevice sd) {
        clone(sd);
    }

    final protected void clone(SerialDevice sd) {
        this.name = sd.name;
        this.originalName = sd.name;
        this.function = "generic";
        this.port = sd.port;
        this.os = sd.os;
    }

    public boolean write(String s) {
        if (this.os == null) {
            return true;
        }

        if (FusorControlServer.config.superVerbose) {
            System.out.println("writing to port " + port.getSystemPortName() + ":" + s);
        }
        byte[] bytes = s.getBytes();
        synchronized (port) {
            try {
                os.write(bytes);
            } catch (IOException ex) {
                System.out.println("SD write exception: " + this.name + ", " + ex.getLocalizedMessage());
                return false;
            }
        }
        return true;
    }

    public boolean command(String s) {
        if (this.os == null) {
            if (FusorControlServer.config.superVerbose) {
                System.out.println("Command to fake device " + name);
            }
            return true;
        }
//        if (FusorControlServer.config.superVerbose) {
        System.out.println("command to device " + name + ": " + s);
//        }

        String cmd = makeCommand(s);

        if (s.equals("GETALL")) {
            write(cmd);
            return true;
        }

        // try this up to 5 times, wait for confirmation
        for (int i = 0; i < 5; i++) {
            try {
                if (!write(cmd)) {
                    return false;
                }
                waitForConfirmation(FusorControlServer.config.cmdTimeOut);
            } catch (Exception e) {
                System.out.println("exc " + e);
                return false;
            }

            if (retrieveConfirmation(s)) {
                return true;
            }
        }
        return false;
    }

    private boolean retrieveConfirmation(String cmd) {
        boolean result;
        synchronized (this.confMonitor) {
            System.out.println("Device: " + this.name + ", cmd: " + cmd + ", conf: " + this.confirmation);
            result = cmd.equals(this.confirmation);
            this.confirmation = null;
        }
        return result;
    }

    private void waitForConfirmation(long ms) throws InterruptedException {
        synchronized (this.confMonitor) {
            this.confMonitor.wait(ms);
        }
    }

    public void setConfirmation(String conf) {
        synchronized (this.confMonitor) {
            this.confirmation = conf;
            this.confMonitor.notify();
        }
    }

    public boolean set(String var, Object val) {
        return command("SET:" + var + ":" + val);
    }

    public void get(String var) {
        command("GET:" + var);
    }

    public void getAll() {
        if (!this.autoStatus) {
            command("GETALL");
        }
    }

    public void autoStatusOn() {
        try {
        command("AUTOSTATUSON");
        } catch (Throwable t) {
            System.out.println("Exception in Autostatus on: "+t);
        }
    }

    public void autoStatusOff() {
        command("AUTOSTATUSOFF");
        this.autoStatus = false;
    }

    public void setStatus(String s) {
        synchronized (this) {
            this.lastStatus = s;
            this.currentStatus = s;
        }
    }

    public String getCurrentStatus() {
        String status;
        synchronized (this) {
            status = this.currentStatus;
            this.currentStatus = null;
        }
        return status;
    }

    public String getLastStatus() {
        return this.lastStatus;
    }

    public boolean isValid() {
        return os != null;
    }

    public void setAutoStatus(boolean auto) {
        this.autoStatus = auto;
    }

    @Override
    public String toString() {
        return "[" + name + (isValid() ? " (" + port.getSystemPortName() + ")" : "") + "]";
    }
}
