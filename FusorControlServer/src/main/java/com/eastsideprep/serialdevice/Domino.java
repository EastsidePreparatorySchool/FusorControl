/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.serialdevice;

import com.eastsideprep.fusorcontrolserver.FusorControlServer;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author gmein
 */
public class Domino extends SerialDevice {

    Thread autoStatusThread;

    Domino(SerialPort p, String name) {
        super(p, name);
        writeDomino(new byte[]{0x04, 0x02});
        writeDomino(new byte[]{0x01, 0x51});
        writeDomino(new byte[]{0x02, 0x01, 0x00, -0x01});
        writeDomino(new byte[]{0x06});
    }

    @Override
    void processSerialData(SerialPortEvent e) {
        byte[] bytes = e.getReceivedData();
        if (bytes.length != 4) {
            System.out.println("Domino: Wrong message length");
            return;
        }

        // make long from bytes
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();
        long count = buffer.getLong();

        // make status string
        long time = System.currentTimeMillis();
        StringBuilder status = new StringBuilder(100);
        status.append("{\"cps\":{\"value\":");
        status.append(count);
        status.append(",\"vartime\":");
        status.append(time);
        status.append("},\"devicetime\":");
        status.append(time);
        status.append("}");

        DeviceManager.instance.recordStatusForDevice(this, time, status.toString());
    }

    // disable the public SerialDevice "write"
    @Override
    public boolean write(String s) {
        throw new UnsupportedOperationException("Domino: Write attempted");
    }

    private boolean writeDomino(byte[] bytes) {
        synchronized (port) {
            try {
                os.write(bytes);
            } catch (IOException ex) {
                System.out.println("Domino write exception: " + this.name + ", " + ex.getLocalizedMessage());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean command(String s) {
        if (FusorControlServer.config.verbose) {
            System.out.println("command to device " + name + ": " + s);
        }

        if (s.equals("GETALL")) {
            writeDomino(new byte[]{0x05}); // get counts
            writeDomino(new byte[]{0x07}); // clear counts
            return true;
        }

        return false;
    }

    @Override
    public boolean set(String var, Object val) {
        throw new UnsupportedOperationException("Domino: Set attempted.");
    }

    @Override
    public void get(String var) {
        if (var.equals("cps")) {
            command("GETALL");
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String toString() {
        return "[DOMINO: " + name + (isValid() ? " (" + port.getSystemPortName() + ")" : "") + "]";
    }

    @Override
    public void autoStatusOn() {
        super.autoStatusOn();
        this.autoStatusThread = new Thread(() -> {
            try {
                while (true) {
                    command("GETALL");
                    Thread.sleep(100);
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                }
            } catch (InterruptedException ex) {
            }
        });
    }

    @Override
    public void autoStatusOff() {
        super.autoStatusOff();
        this.autoStatusThread.interrupt();
    }
}
