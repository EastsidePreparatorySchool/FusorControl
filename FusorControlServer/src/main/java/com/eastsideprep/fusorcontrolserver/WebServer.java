/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

import static spark.Spark.*;
import com.fazecast.jSerialComm.*;
/**
 *
 * @author paul
 */
public class WebServer {
    
    SerialPort arduino;
    
    public WebServer (){
    }
    
    public void init() {
        port(8081);
        staticFiles.location("/public");
        get("/", (req, res) -> "<h1><a href='index.html'>Go to index.html</a></h1>");
        get("/kill", (req, res) -> {stop(); System.out.println("Server ended with /kill"); return "server ended";});
        get("/inita", (req, res) -> serialInit()?"success":"serial init failed");
    }
    
    private boolean serialInit() {
        try {
        arduino = SerialPort.getCommPorts()[0];
        arduino.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
            
            @Override
            public void serialEvent(SerialPortEvent e) {
                
                if(e.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) 
                    return;
                byte[] data = new byte[arduino.bytesAvailable()];
                int count = arduino.readBytes(data, data.length);
                System.out.println("Read " + count + " bytes from arduino");
                handle(new String(data));
            }
        });
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    
    private void handle(String arg) {
        
    }
    
    private void write(String arg) {
        byte[] bytes = arg.getBytes();
        arduino.writeBytes(bytes, bytes.length);
    }
    
}
