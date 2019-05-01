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
        //sets default public location
        staticFiles.location("/public");
        
        //these set all the commands that are going to be sent from the client
        //ones like getstatus will have functions that communicate with the arduino
        get("/", (req, res) -> "<h1><a href='index.html'>Go to index.html</a></h1>");
        get("/kill", (req, res) -> {stop(); System.out.println("Server ended with /kill"); return "server ended";});
        get("/inita", (req, res) -> serialInit()?"success":"serial init failed");
        
        get("/getstatus", "application/json", (req, res) -> getStatus(req, res), new JSONRT());)
        /*get("/getstatus", (req, res) -> {
            boolean arcon = arduino != null;
            long start = System.currentTimeMillis();
            String tmps="";
            String tmpv="";
            write("GETstatusEND");
            String message = "hold";
            //I plan on having a colon for between the key and number,
            //this takes all messages coming in on the buffer until 'statusend' and sends the correct values back to the client
            do {
                int coli = message.indexOf(":");
                if(message.startsWith("tmpv")) {
                    tmpv = message.substring(coli+1);
                } if(message.startsWith("tmps")) {
                    tmps = message.substring(coli+1);
                }                
            } while(!message.equals("statusend")&&System.currentTimeMillis()-start<=serialTimeout);
            String ret = "{ tmps:" + tmps + ", tmpv:" + tmpv + ", arcon:" + (arcon?1:0);
            System.out.println(ret);
            return ret;
        });*/
        
        get("/variac", (req, res) -> {
            Double variacValue = Double.parseDouble(req.queryParams("value"));
            //TODO: paul to connect this to the arduino
            return "set value as " + req.queryParams("value");
        });
    }

    public static Object getStatus(spark.Request req, spark.Response res) {
        boolean arcon = arduino != null;
        long start = System.currentTimeMillis();
        String tmps="";
        String tmpv="";
        write("GETstatusEND");
        String message = "hold";
        //I plan on having a colon for between the key and number,
        //this takes all messages coming in on the buffer until 'statusend' and sends the correct values back to the client
        do {
            int coli = message.indexOf(":");
            if(message.startsWith("tmpv")) {
                tmpv = message.substring(coli+1);
            } if(message.startsWith("tmps")) {
                tmps = message.substring(coli+1);
            }                
        } while(!message.equals("statusend")&&System.currentTimeMillis()-start<=serialTimeout);
        String ret = "{ tmps:" + tmps + ", tmpv:" + tmpv + ", arcon:" + (arcon?1:0);
        System.out.println(ret);
        return ret;
        return myMessages;
    }
    
    private boolean serialInit() {
        //sets up arduino serial communication
        try {
            System.out.println(Arrays.toString(SerialPort.getCommPorts()));
        arduino = SerialPort.getCommPorts()[0];
        arduino.openPort();
            System.out.println("port opened?");
        arduino.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
            
            @Override
            //this is event that happens whenever something gets written from the arduino
            //it takes and characters and puts it into the handle function
            //important note, even if you write something as one message on the arduino, it might not come in all at once here
            //the handle function deals with this by looking for deliniators\
            //the current delineator 
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
        //this function takes all the raw input from the arduino and splits it up correctly into commands
        // 'msgBuffer' is a queue to hold commands
        while(msgBuffer.indexOf("END")!=-1) {
            int semi = msgBuffer.indexOf("END");
            System.out.println(msgBuffer.substring(0, semi));
            msgqueue.add(msgBuffer.substring(0, semi));
            msgBuffer = msgBuffer.substring(semi+1);        
        }
        
        //this while loop is for testing, it should be removed for actual use, otherwise, no other things will be able to get info from the arduino
        while(!msgqueue.isEmpty()) {
            System.out.println(msgqueue.remove());
        }
    }
    
    private void write(String arg) {
        byte[] bytes = arg.getBytes();
        arduino.writeBytes(bytes, bytes.length);
    }
    
}
