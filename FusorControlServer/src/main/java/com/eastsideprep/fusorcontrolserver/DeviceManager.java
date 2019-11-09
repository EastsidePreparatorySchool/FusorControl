package com.eastsideprep.fusorcontrolserver;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceManager {

    //
    // the static part of this class acts as the manager for serial devices
    //
    private SerialPort[] ports;
    private SerialDeviceMap arduinoMap = new SerialDeviceMap();
    private Thread queryThread;
    // keep track of partial messages
    private HashMap<SerialPort, String> bufferState = new HashMap<>();
    private CoreDevices cd;

    private final SerialPortDataListener connectionListener = new SerialPortDataListener() {
        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
        }

        @Override
        public void serialEvent(SerialPortEvent e) {
            if (FusorControlServer.verbose) {
                //System.out.println("  Serial event on port " + e.getSerialPort().getSystemPortName());
            }
            if (e.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                processSerialData(e);
            }
        }
    };

    private void processSerialData(SerialPortEvent e) {
        if (e.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
            return;
        }

        //System.out.println("Serial data available");
        SerialPort port = e.getSerialPort();
        int bytes = port.bytesAvailable();
        if (bytes == 0) {
            return;
        }

        byte[] data = new byte[bytes];
        port.readBytes(data, bytes);

        //System.out.println("Read " + data.length + " bytes from " + e.getSerialPort().getSystemPortName());
        String buffer = new String(data);
        //System.out.println("  string:" + buffer);

        // partial message from last time? put at beginning of buffer
        if (bufferState.containsKey(port)) {
            buffer = bufferState.get(port) + buffer;
        }
        if (FusorControlServer.verbose) {
            //System.out.println("  serial buffer:" + buffer);
        }

        // now look for specific messages from our components
        int start;
        int end = -1;

        start = buffer.indexOf(SerialDevice.FUSOR_RESPONSE_PREFIX);
        if (start != -1) {
            end = buffer.indexOf(SerialDevice.FUSOR_POSTFIX, start);
        }
        //System.out.println("" + start + ", " + end);

        // process all such messages
        while (start != -1 && end > start) {//while there is a complete message to process
            String response = buffer.substring(start + SerialDevice.FUSOR_RESPONSE_PREFIX.length(), end);
            processMessage(response, port);
            buffer = buffer.substring(end + SerialDevice.FUSOR_POSTFIX.length()); //the remainder of the buffer
            start = buffer.indexOf(SerialDevice.FUSOR_RESPONSE_PREFIX);
            if (start != -1) {
                end = buffer.indexOf(SerialDevice.FUSOR_POSTFIX, start);
            }
            //System.out.println("" + start + ", " + end);
        }
        bufferState.put(port, buffer);
    }

    private void processMessage(String response, SerialPort port) {
        long time = System.currentTimeMillis();
        if (FusorControlServer.verbose) {
            System.out.println("  Response from " + port.getSystemPortName() + ":" + response);
        }
        if (response.startsWith(SerialDevice.FUSOR_IDENTIFY + ":")) {
            //System.out.println("  Received identification message: " + response + " from port: " + port.getSystemPortName());
            identify(response.substring(SerialDevice.FUSOR_IDENTIFY.length() + 1), port);
        } else if (response.startsWith(SerialDevice.FUSOR_STATUS + ":")) {
            SerialDevice sd = this.arduinoMap.get(port);
            if (sd != null) {
                String status = response.substring(SerialDevice.FUSOR_STATUS.length() + 1);
                status = DataLogger.makeLogResponse(sd, time, status);
                sd.setStatus(status);
            }
        }
    }

    public CoreDevices init() {
        ports = SerialPort.getCommPorts();

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
        Object semaphore = new Object();
        queryThread = new Thread(() -> queryThreadLoop(semaphore));
        queryThread.start();

        // wait for the query thread to acquire some devices
        // it will notify the semaphore
        synchronized (semaphore) {
            try {
                semaphore.wait(20000);
            } catch (InterruptedException ex) {
            }
        }

        this.cd = new CoreDevices(this);

        // need to be able to fakeCoreDevices this thing away from the Arduinos
        // so make dummy ports if necessary
        if (FusorControlServer.fakeCoreDevices) {
            cd.fakeMissingCoreDevices();
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
        }

        return cd;
    }

    public void shutdown() {
        try {
            queryThread.interrupt();
            queryThread.join(1000);
        } catch (Exception ex) {
        }
    }

    private void queryThreadLoop(Object semaphore) {
        // thread priority below web server and also below logger thread
        // discovering new devices is not that important, after all
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 2);
        try {
            while (!Thread.interrupted()) {
                queryIdentifyAll(semaphore);
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
        }

    }

    private void queryIdentifyAll(Object semaphore) throws InterruptedException {
        //
        // go trough ports, knock on the door
        //

        // filter the list of all ports down to only COM devices,
        // and only ones that are not registered yet. 
        ports = SerialPort.getCommPorts();
        List<SerialPort> portList = new ArrayList<SerialPort>(Arrays.asList(ports));
        portList.removeIf((p) -> !p.getSystemPortName().contains("COM") || arduinoMap.containsPort(p));

        // cut this short if there is nothing new
        if (portList.isEmpty()) {
            return;
        }

        //
        // detect and deal with bluetooth port pairs
        //
        System.out.println("searching for bluetooth port pairs ...");

        for (int i = 0; i < portList.size() - 1; i++) {
            SerialPort port = portList.get(i);
            //System.out.println(port.getDescriptivePortName());

            String name = port.getDescriptivePortName().toLowerCase();
            Pattern p = Pattern.compile("bluetooth.*\\(com([0-9]+)");
            Matcher m = p.matcher(name);
            if (m.find()) {
                // found bluetooth port. try to find the matching port with higher number

                int portNumber = Integer.parseInt(m.group(1));
                name = name.replaceFirst("\\(com" + portNumber + "\\)", "(com" + (portNumber + 1) + ")");
                SerialPort port2 = portList.get(i + 1);

                if (port2 != null && port2.getDescriptivePortName().toLowerCase().equals(name)) {
                    System.out.println("  found bluetooth pair on COM ports: " + portNumber + ", " + (portNumber + 1));
                    System.out.println("  removing read-only bluetooth port " + port.getSystemPortName());
                    // found it. we will take the first port out of the list.
                    // add NullSerialDevice to system, to prevent further querying
                    arduinoMap.put(new NullSerialDevice(port, "<read-only bluetooth port>"));
                    portList.remove(port);
                    --i;
                }
            }
        }

        //
        // now open ports
        //
        for (SerialPort port : portList) {
            System.out.println("opening port: " + port.getSystemPortName());
            port.openPort();

            //System.out.print("port opened. adding listener ...");
            port.addDataListener(connectionListener);
            //System.out.println("listener added.");          
        }

        System.out.println("=================== done opening new ports. waiting for resets ..");
        Thread.sleep(3000);

        for (SerialPort port : portList) {
            try {
                System.out.println("sending identify command to port " + port.getSystemPortName());
                writeToPort(port.getOutputStream(), SerialDevice.makeCommand(SerialDevice.FUSOR_IDENTIFY));
            } catch (Exception ex) {
                System.out.println(ex.getCause());
            }
        }
        System.out.println("=================== done querying new ports. waiting for new devices to identify ...");
        Thread.sleep(1000);

        System.out.println("=================== closing unrecognized ports");
        for (SerialPort port : portList) {
            if (!arduinoMap.containsPort(port)) {
                System.out.println("closing port " + port.getSystemPortName());
                port.closePort();

                // add NullSerialDevice to system, to prevent further querying
                arduinoMap.put(new NullSerialDevice(port, "<unknown>"));
            }
        }

        // signal main thread to go ahead
        synchronized (semaphore) {
            semaphore.notify();
        }
        System.out.println("=================== done querying new ports");
    }

    public void register(SerialDevice sd) {
        // find a unique name 
        String name = sd.name;
        int count = 2;
        while (arduinoMap.get(name) != null) {
            name = sd.name + count;
            count++;
        }
        sd.name = name;

        // register in the map
        arduinoMap.put(sd);
    }

    public void unregister(SerialDevice sd) {
        arduinoMap.remove(sd);
    }

    static void writeToPort(OutputStream os, String arg) throws IOException {
        byte[] bytes = (arg + "END").getBytes();
        os.write(bytes);
        //System.out.println("Wrote '" + arg + "' to an arduino");
    }

    private void identify(String name, SerialPort port) {
        synchronized (this) {

            SerialDevice sd = new SerialDevice(port, name);
            String msg = "";

            sd = specificDevice(sd);

            register(sd);
            System.out.println("  -- new Arduino connected: " + sd.name + " (" + sd.originalName + ", function: " + sd.function + "), on: " + port.getSystemPortName() + msg);
        }

    }

    public SerialDevice get(String name) {
        return arduinoMap.get(name);
    }

    public ArrayList<SerialDevice> getAllDevices() {
        return arduinoMap.getAllDevices();
    }

    public ArrayList<String> getDeviceNames() {
        return arduinoMap.getNames();
    }

    public static SerialDevice specificDevice(SerialDevice sd) {
        switch (sd.name) {
            //
            // core: web server will not start without these
            //
            case "VARIAC":
                sd = new VariacControlDevice(sd);
                sd.function = "Variac control";
                break;
            case "TMP":
                sd = new TMPControlDevice(sd);
                sd.function = "TMP control";
                break;
            case "SOLENOID":
                sd = new SolenoidControlDevice(sd);
                sd.function = "Solenoid control";
                break;

            //
            // non-core: web server may start without them
            //
            case "HVHIGHSIDE":
                sd = new HVHighsideSensor(sd);
                sd.function = "BT HV highside current sensor";
                break;

            default:
                break;
        }

        return sd;
    }

    void getAllStatus() {
        ArrayList<String> deviceNames = arduinoMap.getNames();
        for (String name : deviceNames) {
            SerialDevice sd = this.arduinoMap.get(name);
            //System.out.println("Sending GETALL to "+name);
            sd.getAll();
        }

    }

    String readStatusResults(boolean includeCore) {
        ArrayList<SerialDevice> devices = this.getAllDevices();
        String status = "";

        for (SerialDevice sd : devices) {
            if ((includeCore || !cd.isCoreDevice(sd.name)) && sd.port != null) {
                String s = sd.getLastStatus();
                if (s != null) {
                    status += s + "\n";
                }
            }
        }
        return status;
    }
}
