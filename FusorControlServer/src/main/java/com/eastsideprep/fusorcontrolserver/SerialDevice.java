package com.eastsideprep.fusorcontrolserver;

import com.fazecast.jSerialComm.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class SerialDevice {

    String name;
    String originalName;
    String function;
    OutputStream os;
    SerialPort port;
    ArrayList<String> variables;

    SerialDevice() {
    }

    SerialDevice(SerialPort p, String name) {
        this.name = name;
        this.originalName = name;
        this.function = "generic";
        this.port = p;
        this.os = (p != null ? p.getOutputStream() : null);
        this.variables = new ArrayList<>();
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
        this.variables = sd.variables;
    }

    public void write(String s) {
        if (this.os == null) {
            return;
        }

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
