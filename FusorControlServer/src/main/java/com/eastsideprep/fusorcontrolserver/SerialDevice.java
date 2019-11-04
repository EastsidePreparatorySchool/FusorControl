package com.eastsideprep.fusorcontrolserver;

import com.fazecast.jSerialComm.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class SerialDevice {


    String name;
    OutputStream os;
    SerialPort port;
    ArrayList<String> variables;

    SerialDevice() {
    }

    SerialDevice(SerialPort p, String name) {
        this.name = name;
        this.port = p;
        this.os = p.getOutputStream();
        this.variables = new ArrayList<>();
    }

    final protected void clone(SerialDevice sd) {
        this.name = sd.name;
        this.port = sd.port;
        this.os = sd.os;
        this.variables = sd.variables;
    }

    public void write(String s) {
        byte[] bytes = s.getBytes();
        synchronized (port) {
            try {
                os.write(bytes);
            } catch (IOException ex) {
                // todo: deal with write failure to serial devices
            }
        }
    }
}
