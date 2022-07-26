package com.eastsideprep.serialdevice;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;

public class Arduino extends SerialDevice {

    boolean pingSuspendedFlag = false;

    Arduino(Arduino a) {
        clone(a);
    }

    Arduino(SerialPort sp, String name) {
        super(sp, name);
    }

    @Override
    void processSerialData(SerialPortEvent e) {
        DeviceManager.instance.processSerialDataArduino(e);
    }

    void suspendPing() {
        this.pingSuspendedFlag = true;
    }

    void resumePing() {
        this.pingSuspendedFlag = false;
    }

    boolean pingSuspended() {
        return this.pingSuspendedFlag;
    }
}
