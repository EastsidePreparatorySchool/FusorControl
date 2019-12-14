package com.eastsideprep.fusorcontrolserver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

public class DataLogger {

    private FileWriter writer;
    private Thread loggerThread;
    private DeviceManager dm;
    private String logPath;
    private CamStreamer cs;

    public static String makeLogResponse(SerialDevice sd, long time, String response) {
        return "{\"device\":\"" + sd.name + "\",\"data\":" + response + ",\"servertime\":" + time + "}";

    }

    public void init(DeviceManager dm, CamStreamer cs) throws IOException {
        this.dm = dm;
        
        //creates time stamp and appends to .json file, then creates a writer and inits headers
        makeLogPath();
        open();
        this.cs = cs;
        //cs.startRecording(this.logPath+"cam");

        loggerThread = new Thread(() -> loggerThreadLoop());
        if (!FusorControlServer.config.noLog) {
            loggerThread.start();
        }
    }

    void shutdown() {
        try {
            //cs.stopRecording();
            loggerThread.interrupt();
            loggerThread.join(500);
            close();
        } catch (Exception ex) {
        }
    }

    void loggerThreadLoop() {
        // priority a little below normal, so that web requests come first
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
        try {
            while (!Thread.interrupted()) {
                //dm.getAllStatus();
                Thread.sleep(1000 / FusorControlServer.config.logFreq);
                logAll();
            }
        } catch (InterruptedException e) {
        }
    }

    void logAll() {
        ArrayList<SerialDevice> devices = dm.getAllDevices();
        for (SerialDevice sd : devices) {
            String s = sd.getCurrentStatus();
            if (s != null) {
                try {
                    write(s);
                } catch (IOException ex) {
                    System.out.println("DataLogger:write:ioexception: " + ex);
                }
            }
        }
    }

    private void open() throws IOException {
        Date date = new Date();
        long millis = System.currentTimeMillis();
        Instant instant1 = date.toInstant();
        String ts = instant1.toString();
        String fileName = logPath + "fusor-" + ts.replace(":", "-").replace(".", "-") + ".json";
        writer = new FileWriter(fileName);
        writer.append("{\"base-timestamp\":" + millis + ",\"instant\":\"" + ts + "\",\"log\":[\n");
        writer.flush();
    }

    private void write(String element) throws IOException {
        writer.append(element);
        writer.append(",\n");
        writer.flush();
    }

    private void close() throws IOException {
        writer.append("  {}\n]}\n");
        writer.flush();
        writer.close();
    }

    public void makeLogPath() {
        this.logPath = System.getProperty("user.dir") + System.getProperty("file.separator") + "logs";
        if (FusorControlServer.config.logPath != null) {
            this.logPath = FusorControlServer.config.logPath;
        }

        // take of ending slashes for a moment
        if (logPath.endsWith(System.getProperty("file.separator"))) {
            logPath = logPath.substring(0, logPath.length()-1);
        }
        // make sure log path exists
        createFolder(this.logPath);
        
        // put the slash back on
        logPath +=  System.getProperty("file.separator");
        System.out.println("Log path is "+logPath);
    }

    public static void createFolder(String folder) {
        File file = new File(folder);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                System.err.println("Log folder " + folder + "does not exist and cannot be created");
            }
        }
    }
}
