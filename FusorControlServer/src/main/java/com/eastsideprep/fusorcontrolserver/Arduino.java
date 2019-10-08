/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

import com.fazecast.jSerialComm.*;
import java.io.OutputStream;

/**
 *
 * @author Liam
 */
public class Arduino {
    String type = "unknown";
    OutputStream os;
    SerialPort port;
    
    Arduino(SerialPort p){
        this.port = p;
        this.os = p.getOutputStream();
    }
}
