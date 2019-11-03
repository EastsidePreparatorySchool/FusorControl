package com.eastsideprep.fusorcontrolserver;

import com.fazecast.jSerialComm.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SerialDevice extends Device {

    //
    // the static part of this class acts as the manager for serial devices
    //
    private static SerialPort[] ports;
    private static HashMap<SerialPort, SerialDevice> portToArduino = new HashMap<>();
    private static DeviceManager dm;

    private static final SerialPortDataListener connectionListener = new SerialPortDataListener() {
        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
        }

        // keep track of partial messages
        HashMap<SerialPort, String> bufferState = new HashMap<>();

        @Override
        public void serialEvent(SerialPortEvent e) {
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
            final String PREFIX = "FusorResponse[";
            final String POSTFIX = "]END";
            int start;
            int end = -1;

            start = buffer.indexOf(PREFIX);
            if (start != -1) {
                end = buffer.indexOf(POSTFIX, start);
            }
            //System.out.println("" + start + ", " + end);

            // process all such messages
            while (start != -1 && end > start) {//while there is a complete message to process
                String response = buffer.substring(start + PREFIX.length(), end);
                processMessage(response, port);
                buffer = buffer.substring(end + POSTFIX.length()); //the remainder of the buffer
                start = buffer.indexOf(PREFIX);
                if (start != -1) {
                    end = buffer.indexOf(POSTFIX, start);
                }
                //System.out.println("" + start + ", " + end);
            }
            bufferState.put(port, buffer);
        }
    };

    static private void processMessage(String response, SerialPort port) {
        System.out.println("Received message: " + response + " from port: " + port.getSystemPortName());
        final String IDENTIFY = "IDENTIFY:";
        if (response.startsWith(IDENTIFY)) {
            identify(response.substring(IDENTIFY.length()), port);
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
        queryIdentifyAll();
    }

    static private void queryIdentifyAll() {
        //
        // go trough ports, knock on the door
        //

        List<SerialPort> portList = Arrays.asList(ports);
        portList.removeIf((p) -> !p.getDescriptivePortName().contains("COM")
                || SerialDevice.portToArduino.containsKey(p.getSystemPortName()));

        for (SerialPort port : portList) {
            System.out.print("opening port: " + port.getSystemPortName() + "...");
            port.openPort();
            System.out.print("port opened. adding listener ...");
            port.addDataListener(connectionListener);
            System.out.println("listener added.");
        }

        System.out.println("=================== done opening new ports. waiting 5s");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }

        final String FUSORCOMMANDPREFIX = "FusorCommand[";
        final String FUSORCOMMANDPOSTFIX = "]END";
        final String FUSORCOMMANDIDENTIFY = "IDENTIFY";

        for (SerialPort port : portList) {
            try {
                System.out.println("sending identify command to port " + port.getDescriptivePortName());
                writeToPort(port.getOutputStream(), FUSORCOMMANDPREFIX + FUSORCOMMANDIDENTIFY + FUSORCOMMANDPOSTFIX);
            } catch (IOException ex) {
                System.out.println(ex.getCause());
            }
        }
        System.out.println("=================== done querying new ports. waiting 2s");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }

        System.out.println("=================== closing unrecognized ports");
        for (SerialPort port : portList) {
            if (!SerialDevice.portToArduino.containsKey(port)) {
                System.out.println("closing port "+port.getSystemPortName());
                port.closePort();
            }
        }

    }

    static void register(SerialDevice sd, SerialPort port) {
        SerialDevice.portToArduino.put(port, sd);
    }

    static void unregister(SerialDevice sd) {
        SerialDevice.portToArduino.remove(sd.port);
    }

    static void writeToPort(OutputStream os, String arg) throws IOException {
        byte[] bytes = (arg + "END").getBytes();
        os.write(bytes);
        //System.out.println("Wrote '" + arg + "' to an arduino");
    }

    private static void identify(String name, SerialPort port) {

        System.out.print(" -- new Arduino connected: " + name + ", on: " + port.getDescriptivePortName());
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
        register(sd, port);

    }

//
// these are the member variables of the actual serial device
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
        try {
            os.write(bytes);
        } catch (IOException ex) {
            // todo: deal with write failure to serial devices
        }
    }
}
