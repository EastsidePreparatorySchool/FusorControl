package com.eastsideprep.fusorcontrolserver;

import com.fazecast.jSerialComm.*;
import java.io.IOException;
import java.io.OutputStream;

public class SerialDevice {

    String name;
    String originalName;
    String function;
    private OutputStream os;
    SerialPort port;
    private String lastStatus;
    private String currentStatus;
    private boolean autoStatus = false;

    public final static String FUSOR_COMMAND_PREFIX = "FusorCommand[";
    public final static String FUSOR_RESPONSE_PREFIX = "FusorResponse[";
    public final static String FUSOR_POSTFIX = "]END";

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

    public void write(String s) {
        if (this.os == null) {
            return;
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
            }
        }
    }

    public void command(String s) {
        if (this.os == null) {
            return;
        }
        //if (FusorControlServer.config.superVerbose || (!s.equals("GETALL"))) {
            System.out.println("command to device " + name + ": " + s);
        //}
        write(SerialDevice.makeCommand(s));
    }

    public void set(String var, Object val) {
        command("SET:" + var + ":" + val);
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
        command("AUTOSTATUSON");
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
}
