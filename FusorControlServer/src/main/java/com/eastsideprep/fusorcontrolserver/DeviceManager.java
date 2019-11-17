package com.eastsideprep.fusorcontrolserver;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class DeviceManager {

    //
    // the static part of this class acts as the manager for serial devices
    //
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

        if (FusorControlServer.superVerbose) {
            System.out.println("Read " + data.length + " bytes from " + e.getSerialPort().getSystemPortName());
        }
        String buffer = new String(data);
        if (FusorControlServer.superVerbose) {
            //System.out.println("  string:" + buffer);
        }

        // partial message from last time? put at beginning of buffer
        if (bufferState.containsKey(port)) {
            buffer = bufferState.get(port) + buffer;
        }
        if (FusorControlServer.superVerbose) {
            System.out.println("  serial buffer:" + buffer);
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
        if (FusorControlServer.superVerbose) {
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
        SerialPort[] ports = SerialPort.getCommPorts();

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
                semaphore.wait(30000);
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
            //while (!Thread.interrupted()) {
            queryIdentifyAll(semaphore);
            Thread.sleep(5000);
            //}
        } catch (InterruptedException e) {
        }

    }

    private void queryIdentifyAll(Object semaphore) throws InterruptedException {
        //
        // go trough ports, knock on the door
        //

        // need to be adapted to machine this is running on
        String[] ignorePorts = {
            "Standard Serial over Bluetooth link (COM11)"
        };

        // filter the list of all ports down to only COM devices,
        // and only ones that are not registered yet. 
        SerialPort[] ports = SerialPort.getCommPorts();
        List<SerialPort> portList = new ArrayList<>(Arrays.asList(ports));
        portList.removeIf((p) -> ((!p.getSystemPortName().contains("COM"))
                || arduinoMap.containsPort(p)
                || (Arrays.binarySearch(ignorePorts, p.getDescriptivePortName()) >= 0)
                || (p.getDescriptivePortName().toLowerCase().contains("bluetooth") && FusorControlServer.noBlueTooth)));

        //
        // sort by COM port number
        //
        Collections.sort(portList, (a, b) -> (getPortNumber(a) - getPortNumber(b)));

        //
        // filter out bluetooth pairs
        //
        if (portList.size() > 1) {
            System.out.println("scanning for bluetooth port pairs ...");
            for (int i = 0; i < portList.size() - 1; i++) {
                SerialPort p = portList.get(i);
                // This is list sorted by descriptive name, so two bluetooth ports would be next to each other, identical except for the number
                if (p.getDescriptivePortName().toLowerCase().contains("bluetooth")) {
                    int com = p.getDescriptivePortName().toLowerCase().indexOf("com");
                    String name = p.getDescriptivePortName().substring(0, com);
                    SerialPort pB = portList.get(i + 1);
                    String nextName = pB.getDescriptivePortName().substring(0, com);
                    if (name.equalsIgnoreCase(nextName) && ((getPortNumber(pB) - getPortNumber(p)) == 1)) {
                        // Found bluetooth pair of COM ports. Now which is incoming, and which is outgoing?
                        // We need the outgoing one. To find out, we will write to *both* and see who dies.
                        // Get that going on two threads and wait ...
                        SerialPort[] wrongOne = {null}; // this needs to be an array so I can change it from the lambdas. Annoying.
                        Thread threadA = new Thread(() -> {
                            try {
                                p.setComPortTimeouts​(SerialPort.TIMEOUT_NONBLOCKING, 0, 100);
                                p.openPort();
                                //System.out.println("" + p.getOutputStream());
                                writeToPort(p, "*");
                                wrongOne[0] = pB;
                            } catch (Exception ex) {
                                //System.out.println("Exception on bluetooth " + p.getSystemPortName());
                                //System.out.println(ex);
                            }
                        });
                        threadA.start();
                        Thread threadB = new Thread(() -> {
                            try {
                                pB.setComPortTimeouts​(SerialPort.TIMEOUT_NONBLOCKING, 0, 100);
                                pB.openPort();
                                //System.out.println("" + pB.getOutputStream());
                                writeToPort(pB, "*");
                                pB.setComPortTimeouts​(SerialPort.TIMEOUT_NONBLOCKING, 0, 100);
                                wrongOne[0] = p;
                            } catch (Exception ex) {
                                //System.out.println("Exception on bluetooth " + pB.getSystemPortName());
                                //System.out.println(ex);
                            }
                        });
                        threadB.start();
                        // wait for threads to finish one way or another
                        threadA.join(1000);
                        threadB.join(1000);
                        // time to pick up the pieces. if the port was set, remove it from the list.
                        if (wrongOne[0] == null) {
                            // both are duds
                            System.out.println("  - removing deaf port pair "+p.getSystemPortName()+", "+pB.getSystemPortName());
                            p.closePort();
                            portList.remove(p);
                            pB.closePort();
                            portList.remove(pB);
                            i--;
                        } else {
                            // remove the one that doesn't work
                            System.out.println("  - removing deaf port " + wrongOne[0].getSystemPortName());
                            wrongOne[0].closePort();
                            portList.remove(wrongOne[0]);
                        }
                    }
                }
            }
        }

        // cut this short if there is nothing new
        if (portList.isEmpty()) {
            return;
        }

        //
        // now open ports
        //
        Thread[] threads = new Thread[portList.size()];
        int i = 0;
        for (SerialPort port : portList) {
            System.out.println("opening port: " + port.getSystemPortName());
            port.setComPortParameters(115200, 8, 1, SerialPort.NO_PARITY);
            threads[i] = new Thread(() -> {
                try {
                    port.setComPortTimeouts​(SerialPort.TIMEOUT_NONBLOCKING, 0, 100);
                    port.openPort();
                    port.addDataListener(connectionListener);
                    Thread.sleep(2000);
                    writeToPort(port, SerialDevice.makeCommand(SerialDevice.FUSOR_IDENTIFY));
                    Thread.sleep(2000);
                } catch (Exception e) {
                    System.out.println("open exception: " + e);
                }
            });
            threads[i].start();
            i++;
        }
        for (Thread t : threads) {
            t.join();
        }

        //
        // identify
        //
        portList.removeIf((p) -> arduinoMap.containsPort(p));
        if (!portList.isEmpty()) {
            System.out.println("=================== querying ports that have not reported yet...");

            for (SerialPort port : portList) {
                try {
                    System.out.println("sending identify command to port " + port.getSystemPortName());
                    writeToPort(port, SerialDevice.makeCommand(SerialDevice.FUSOR_IDENTIFY));
                } catch (Exception ex) {
                    System.out.println("Query serial write exception cause: " + ex.getCause());
                }
            }
            System.out.println("=================== done querying new ports. waiting for new devices to identify ...");
            Thread.sleep(2000);
        }
        //
        // second round for those who did not answer
        //
        portList.removeIf((p) -> arduinoMap.containsPort(p));
        if (!portList.isEmpty()) {
            System.out.println("=================== second round of querying new ports...");
            for (SerialPort port : portList) {
                try {
                    System.out.println("sending second identify command to port " + port.getSystemPortName());
                    writeToPort(port, SerialDevice.makeCommand(SerialDevice.FUSOR_IDENTIFY));
                } catch (Exception ex) {
                    //System.out.println("Exception cause: "+ex.getCause());
                }
            }
            System.out.println("=================== waiting for new devices to identify ...");
            Thread.sleep(5000);
        }

        System.out.println("=================== closing unrecognized ports");
        for (SerialPort port : portList) {
            if (!arduinoMap.containsPort(port)) {
                System.out.println("closing port " + port.getSystemPortName());
                port.closePort();

                // add NullSerialDevice to system, to prevent further querying
                arduinoMap.put(new NullSerialDevice(port, "<unknown>"));
            }
        }
        System.out.println("=================== done collecting new ports, total devices now registered: " + this.arduinoMap.validDeviceCount());

        // signal main thread to go ahead
        synchronized (semaphore) {
            semaphore.notify();
        }
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

    static void writeToPort(SerialPort port, String arg) throws IOException {
        byte[] bytes = arg.getBytes();
        if (port.getOutputStream() != null) {
            port.getOutputStream().write(bytes);
            if (FusorControlServer.superVerbose) {
                System.out.println("Wrote '" + arg + "' to port " + port.getSystemPortName());
            }
        }
    }

    private void identify(String name, SerialPort port) {
        synchronized (this) {
            if (!arduinoMap.containsPort(port)) {
                SerialDevice sd = new SerialDevice(port, name);
                String msg = "";

                sd = specificDevice(sd);

                register(sd);
                System.out.println("  -- new Arduino connected: " + sd.name + " (" + sd.originalName + ", function: " + sd.function + "), on: " + port.getSystemPortName() + msg);
            }
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
        ArrayList<SerialDevice> devices = arduinoMap.getAllDevices();
        for (SerialDevice sd : devices) {
            //System.out.println("Sending GETALL to "+name);
            sd.getAll();
        }

    }

    String readStatusResults(boolean includeCore) {
        ArrayList<SerialDevice> devices = this.getAllDevices();
        String status = "";
        devices.sort((a, b) -> ((cd.isCoreDevice(b.name) ? 1 : 0) - (cd.isCoreDevice(a.name) ? 1 : 0)));
        for (SerialDevice sd : devices) {
            if ((includeCore || !cd.isCoreDevice(sd.name)) && sd.port != null) {
                String s = sd.getLastStatus();
                if (s != null) {
                    status += s + ",\n";
                }
            }
        }

        status = "[" + status + "{\"status\":\"complete: " + ((new Date()).toInstant().toString()) + "\"}]";
        return status;
    }

    static private int getPortNumber(SerialPort p) {
        int result;
        try {
            String name = p.getSystemPortName();
            result = Integer.parseInt(name.substring(3));
        } catch (Exception e) {
            result = 0;
        }
        return result;
    }
}
