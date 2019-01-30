/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

import static spark.Spark.*;
import com.fazecast.jSerialComm.*;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
/**
 *
 * @author paul
 */
public class WebServer {
    
    SerialPort arduino;
    String msgBuffer;
    Queue msgqueue;
    final int serialTimeout = 1500;
    
    public WebServer (){
        msgqueue = new LinkedBlockingQueue<String>();
    }
    
    public void init() {
        port(8080); //switch to 80 for regular use
        staticFiles.location("/public");
        get("/", (req, res) -> "<h1><a href='index.html'>Go to index.html</a></h1>");
        get("/kill", (req, res) -> {stop(); System.out.println("Server ended with /kill"); return "server ended";});
        get("/inita", (req, res) -> serialInit()?"success":"serial init failed");
        get("/getstatus", (req, res) -> {
            boolean arcon = arduino != null;
            long start = System.currentTimeMillis();
            write("$status;");
            String message = "hold";
            do {
                
            } while(!message.equals("statusend"));
            return "";
        });
    }
    
    private boolean serialInit() {
        try {
            System.out.println(Arrays.toString(SerialPort.getCommPorts()));
        arduino = SerialPort.getCommPorts()[0];
        arduino.openPort();
            System.out.println("port opened?");
        arduino.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
            
            @Override
            public void serialEvent(SerialPortEvent e) {
                //System.out.println(e.getEventType());
                if(e.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) 
                    return;
                byte[] data = new byte[arduino.bytesAvailable()];
                int count = arduino.readBytes(data, data.length);
                System.out.println("Read " + count + " bytes from arduino");
                handle(new String(data));
            }
        });
        } catch (Exception e) {
            System.out.println("there was an error");
            return false;
        }
        return true;
    }
    
    private void handle(String arg) {
        //System.out.println(arg);
        msgBuffer += arg;
        while(msgBuffer.indexOf(";")!=-1) {
            int semi = msgBuffer.indexOf(";");
            System.out.println(msgBuffer.substring(0, semi));
            msgqueue.add(msgBuffer.substring(0, semi));
            msgBuffer = msgBuffer.substring(semi+1);        
        }
        while(!msgqueue.isEmpty()) {
            System.out.println(msgqueue.remove());
        }
    }
    
    private void write(String arg) {
        byte[] bytes = arg.getBytes();
        arduino.writeBytes(bytes, bytes.length);
    }
    
}
