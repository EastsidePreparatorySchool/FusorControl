/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.serialdevice;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;

/**
 *
 * @author gmein
 */
public class Arduino extends SerialDevice {

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
    
    
    
}
