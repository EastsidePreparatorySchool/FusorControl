package com.eastsideprep.fusorcontrolserver;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

public class DataLogger {

    FileWriter writer;
    Thread loggerThread;
    DeviceManager dm;

    public static String makeLogResponse(SerialDevice sd, long time, String response) {
        return "  {\"device\":\"" + sd.name + "\",\"servertime\":" + time + ",\"data\":" + response + "}";

    }

    public void init(DeviceManager dm) throws IOException {
        this.dm = dm;
        //creates time stamp and appends to .json file, then creates a writer and inits headers

        open();

        loggerThread = new Thread(() -> loggerThreadLoop());
        if (!FusorControlServer.noLog) {
            loggerThread.start();
        }
    }

    void shutdown() {
        try {
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
                dm.getAllStatus();
                Thread.sleep(1000/FusorControlServer.logFreq);
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
        String fileName = "fusor-" + ts.replace(":", "-").replace(".", "-") + ".json";
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
}
