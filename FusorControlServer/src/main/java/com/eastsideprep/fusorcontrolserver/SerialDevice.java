package com.eastsideprep.fusorcontrolserver;

import com.fazecast.jSerialComm.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SerialDevice extends Device {

    //
    // the static part of this class acts as the manager for serial devices
    //
    private final static String FUSOR_RESPONSE_PREFIX = "FusorResponse[";
    private final static String FUSOR_COMMAND_PREFIX = "FusorCommand[";
    private final static String FUSOR_POSTFIX = "]END";

    private final static String FUSOR_COMMAND_IDENTIFY = "IDENTIFY";
    private final static String FUSOR_RESPONSE_IDENTIFY = "IDENTIFY:";

    private static SerialPort[] ports;
    private static PortMap portToArduino = new PortMap();
    private static DeviceManager dm;
    private static Thread queryThread;
    // keep track of partial messages
    private static HashMap<SerialPort, String> bufferState = new HashMap<>();

    private static final SerialPortDataListener connectionListener = new SerialPortDataListener() {
        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
        }

        @Override
        public void serialEvent(SerialPortEvent e) {
            //System.out.println("Serial event happened");
            if (e.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                processSerialData(e);
            }
        }
    };


 
    private static void processSerialData(SerialPortEvent e) {
        //System.out.println("Serial event happened");

        if (e.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
            return;
        }
        //System.out.println("Serial data available");
        SerialPort port = e.getSerialPort();
        byte[] data = new byte[port.bytesAvailable()];
        port.readBytes(data, data.length);

        //System.out.println("Read " + data.length + " bytes from " + e.getSerialPort().getSystemPortName());
        String buffer = new String(data);

        // partial message from last time? put at beginning of buffer
        if (bufferState.containsKey(port)) {
            buffer = bufferState.get(port) + buffer;
        }
        //System.out.println("Serial buffer:" + buffer);

        // now look for specific messages from our components
        int start;
        int end = -1;

        start = buffer.indexOf(FUSOR_RESPONSE_PREFIX);
        if (start != -1) {
            end = buffer.indexOf(FUSOR_POSTFIX, start);
        }
        //System.out.println("" + start + ", " + end);

        // process all such messages
        while (start != -1 && end > start) {//while there is a complete message to process
            String response = buffer.substring(start + FUSOR_RESPONSE_PREFIX.length(), end);
            processMessage(response, port);
            buffer = buffer.substring(end + FUSOR_POSTFIX.length()); //the remainder of the buffer
            start = buffer.indexOf(FUSOR_RESPONSE_PREFIX);
            if (start != -1) {
                end = buffer.indexOf(FUSOR_POSTFIX, start);
            }
            //System.out.println("" + start + ", " + end);
        }
        bufferState.put(port, buffer);
    }

    static private void processMessage(String response, SerialPort port) {
        System.out.println("Received message: " + response + " from port: " + port.getSystemPortName());
        if (response.startsWith(FUSOR_RESPONSE_IDENTIFY)) {
            identify(response.substring(FUSOR_RESPONSE_IDENTIFY.length()), port);
        }
    }

    static public void init(DeviceManager dm) {
        SerialDevice.dm = dm;
        SerialDevice.ports = SerialPort.getCommPorts();

        //
        // list current ports on system
        //
        System.out.println("Serial Ports Connected: ");
        for (SerialPort port : ports) {
            System.out.println("  " + port.getDescriptivePortName());
        }
        System.out.println("");

        //
        // go trough ports, knock on the door
        //
        queryThread = new Thread(() -> queryThreadLoop());
        queryThread.start();
    }

    public static void shutdown() {
        try {
            queryThread.interrupt();
            queryThread.join(1000);
        } catch (Exception ex) {
        }
    }

    static private void queryThreadLoop() {
        while (!Thread.interrupted()) {

            try {
                queryIdentifyAll();
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }

    }

    static private void queryIdentifyAll() throws InterruptedException {
        //
        // go trough ports, knock on the door
        //

        // filter the list of all ports down to only COM devices,
        // and only ones that are not registered yet. 
        List<SerialPort> portList = Arrays.asList(ports);
        portList.removeIf((p) -> !p.getSystemPortName().contains("COM")
                || portToArduino.containsKey(p));

        for (SerialPort port : portList) {
            System.out.print("opening port: " + port.getSystemPortName() + "...");
            port.openPort();
            System.out.print("port opened. adding listener ...");
            port.addDataListener(connectionListener);
            System.out.println("listener added.");
        }

        System.out.println("=================== done opening new ports. waiting 5s");
        Thread.sleep(5000);

        for (SerialPort port : portList) {
            try {
                System.out.println("sending identify command to port " + port.getSystemPortName());
                writeToPort(port.getOutputStream(), FUSOR_COMMAND_PREFIX + FUSOR_COMMAND_IDENTIFY + FUSOR_POSTFIX);
            } catch (IOException ex) {
                System.out.println(ex.getCause());
            }
        }
        System.out.println("=================== done querying new ports. waiting 2s");
        Thread.sleep(2000);

        System.out.println("=================== closing unrecognized ports");
        for (SerialPort port : portList) {
            if (!portToArduino.containsKey(port)) {
                System.out.println("closing port " + port.getSystemPortName());
                port.closePort();
            }
        }

    }

    static void register(SerialDevice sd) {
        portToArduino.put(sd);
    }

    static void unregister(SerialDevice sd) {
        portToArduino.remove(sd);
    }

    static void writeToPort(OutputStream os, String arg) throws IOException {
        byte[] bytes = (arg + "END").getBytes();
        os.write(bytes);
        //System.out.println("Wrote '" + arg + "' to an arduino");
    }

    private static void identify(String name, SerialPort port) {

        System.out.print(" -- new Arduino connected: " + name + ", on: " + port.getSystemPortName());
        SerialDevice sd = new SerialDevice(port, name);

        switch (name) {
            case "VARIAC":
                sd = new VariacControlDevice(sd);
                System.out.println(" -- recognized as variac Arduino");
                break;
            case "TMP":
                sd = new TMPControlDevice(sd);
                System.out.println(" -- recognized as TMP Arduino");
                break;
            case "SOLENOID":
                sd = new SolenoidControlDevice(sd);
                System.out.println(" -- recognized as solenoid Arduino");
                break;
            case "GENERICTEST":
                System.out.println(" -- recognized as generic test Arduino");
                break;
            default:
                System.out.println("");
                break;
        }
        register(sd);

    }

    //
    // these are the member variables of the actual serial device instance
    //
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

    @Override
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
