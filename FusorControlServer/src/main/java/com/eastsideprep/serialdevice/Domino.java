/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.serialdevice;

import com.eastsideprep.fusorcontrolserver.FusorControlServer;
import static com.eastsideprep.serialdevice.DeviceManager.writeToPort;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author gmein
 */
public class Domino extends SerialDevice {

    Thread autoStatusThread;
    final private static String domino = "Domino 5.4 Driver Board"; // description of pseudo-port for it
    byte[] buffer = new byte[4];
    int count = 0;

    Domino(SerialPort p, String name, SerialPortDataListener connectionListener) {
        super(p, name);
        port.setComPortParameters(115200, 8, 1, SerialPort.NO_PARITY);
        try {
            port.setComPortTimeouts​(SerialPort.TIMEOUT_NONBLOCKING, 0, 100);
            port.openPort();
            port.addDataListener(connectionListener);
            os = port.getOutputStream();
            writeDomino(new byte[]{0x04, 0x02});
            writeDomino(new byte[]{0x01, 0x51});
            writeDomino(new byte[]{0x02, 0x01, 0x00, -0x01});
            writeDomino(new byte[]{0x06});
        } catch (Exception e) {
            System.out.println("Domino: open exception: " + e);
        }

        autoStatusOn();
        command("GETALL");

    }

    public static boolean isDominoPort(String name) {
        return name.equals(domino);
    }

    @Override
    void processSerialData(SerialPortEvent e) {
        byte[] bytes = e.getReceivedData();
        System.out.println("Domino: received " + bytes.length + " bytes");
        if (bytes.length != 4) {
            // check whether it fits
            if (bytes.length + count > 4) {
                System.out.println("Domino: Wrong message length");
                count = 0;
                return;
            }
            // append to buffer
            for (int i = 0; i < bytes.length; i++, count++) {
                buffer[count] = bytes[i];
            }

            // might nbot be complete
            if (count < 4) {
                return;
            }
            
            // but maybe it is
            bytes = buffer;
            count = 0;
        }

        // make long from bytes
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long count = Integer.toUnsignedLong(buffer.getInt());

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
        if (this.autoStatusThread != null) {
            return;
        }
        synchronized (this) {
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
            this.autoStatusThread.start();
        }
    }

    @Override
    public void autoStatusOff() {
        synchronized (this) {
            super.autoStatusOff();
            this.autoStatusThread.interrupt();
            this.autoStatusThread = null;
        }
    }
}
