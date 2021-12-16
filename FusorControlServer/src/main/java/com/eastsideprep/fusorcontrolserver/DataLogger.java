package com.eastsideprep.fusorcontrolserver;

import com.eastsideprep.cameras.CamStreamer;
import com.eastsideprep.fusorweblog.FusorWebLogEntry;
import com.eastsideprep.serialdevice.DeviceManager;
import com.eastsideprep.serialdevice.SerialDevice;
import com.eastsideprep.weblog.WebLog;
import com.eastsideprep.weblog.WebLogEntry;
import com.eastsideprep.weblog.WebLogObserver;
import org.json.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;

public class DataLogger {

    private FileWriter writer;
    private Thread loggerThread;
    private DeviceManager dm;
    public String logPath;
    private CamStreamer cs;
    public long baseTime;
    public String currentFile;

    public static String makeLogResponse(SerialDevice sd, long time, String response) {
        return "{\"device\":\"" + sd.name + "\",\"data\":" + response + ",\"servertime\":" + time + "}";

    }

    public static String makeLogResponse(String device, long time, String response) {
        return "{\"device\":\"" + device + "\",\"data\":" + response + ",\"servertime\":" + time + "}";

    }

    public void init(DeviceManager dm, CamStreamer cs, String customName) {
        this.dm = dm;
        this.currentFile = "";

        if (!FusorControlServer.config.noLog) {
            // make sure the folder exists
            makeLogPath();

            // create time info for logfile and cam files
            String ts = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
            String fileName = makeFileName(ts);

            // create logfile
            this.baseTime = WebServer.log.baseTime;
            try {
                open(fileName + (customName != null ? "_" + customName : ""), ts);
            } catch (IOException ex) {
                System.out.println("Could not create log file, no permanent log on this run");
                writer = null;
            }
            this.cs = cs;
            if (FusorControlServer.config.saveProcessedVideo) {
                cs.startRecording(fileName + "_cam_", this.baseTime, customName);
            }

            // and go
            loggerThread = new Thread(() -> loggerThreadLoop());
            loggerThread.start();
        }
    }

    void shutdown() {
        try {
            // stop the camera
            cs.stopRecording();

            // stop the logger thread
            loggerThread.interrupt();
            loggerThread.join(500);

            // flush and close the log file
            close();
        } catch (Exception ex) {
        }
    }

    static StringBuilder startPseudoDeviceEntry(int size) {
        StringBuilder sb = new StringBuilder(size);
        sb.append("{");
        return sb;
    }

    static void addPseudoDeviceIntVariable(StringBuilder sb, String var, int val, long varTime) {
        sb.append("\"");
        sb.append(var);
        sb.append("\":{\"value\":");
        sb.append(val);
        sb.append(",\"vartime\":");
        sb.append(varTime);
        sb.append("},");
    }

    static void addPseudoDeviceDoubleVariable(StringBuilder sb, String var, double val, long varTime) {
        sb.append("\"");
        sb.append(var);
        sb.append("\":{\"value\":");
        sb.append(val);
        sb.append(",\"vartime\":");
        sb.append(varTime);
        sb.append("},");
    }

    static void addPseudoDeviceStringVariable(StringBuilder sb, String var, String val, long varTime) {
        sb.append("\"");
        sb.append(var);
        sb.append("\":{\"value\":\"");
        sb.append(val);
        sb.append("\",\"vartime\":");
        sb.append(varTime);
        sb.append("},");
    }

    static String closePseudoDeviceEntry(StringBuilder sb, long deviceTime) {
        sb.append("\"devicetime\":");
        sb.append(deviceTime);
        sb.append("}");
        return sb.toString();
    }

    static String makeHeartbeatDeviceText(double val, long millis) {
        StringBuilder sb = startPseudoDeviceEntry(500);
        addPseudoDeviceDoubleVariable(sb, "beat", val, millis);
        addPseudoDeviceIntVariable(sb, "logsize", WebLog.instance.getLogSize() / 1000, millis);
        return closePseudoDeviceEntry(sb, millis);
    }

    static String makeCommentDeviceText(String observer, String ip, String text, long millis) {
        StringBuilder sb = DataLogger.startPseudoDeviceEntry(1000);
        DataLogger.addPseudoDeviceStringVariable(sb, "observer", observer, millis);
        DataLogger.addPseudoDeviceStringVariable(sb, "ip", ip, millis);
        DataLogger.addPseudoDeviceStringVariable(sb, "text", (
                // encode user-submitted data as Base 64 so comments containing
                // quotes won't cause issues in the JOSN file
                java.util.Base64.getEncoder().encodeToString(
                    text.getBytes(StandardCharsets.UTF_8)
                )), millis);
        return DataLogger.closePseudoDeviceEntry(sb, millis);
    }
    
    static String makeEmergencyStopDeviceText(String observer, String ip, long millis) {
        StringBuilder sb = DataLogger.startPseudoDeviceEntry(1000);
        DataLogger.addPseudoDeviceStringVariable(sb, "observer", observer, millis);
        DataLogger.addPseudoDeviceStringVariable(sb, "ip", ip, millis);
        DataLogger.addPseudoDeviceStringVariable(sb, "text", "[emergency stop]", millis);
        return DataLogger.closePseudoDeviceEntry(sb, millis);
    }

    static String makeAdminCommandText(String name, String ip, String command, long millis) {
        StringBuilder sb = DataLogger.startPseudoDeviceEntry(1000);
        DataLogger.addPseudoDeviceStringVariable(sb, "observer", name, millis);
        DataLogger.addPseudoDeviceStringVariable(sb, "ip", ip, millis);
        DataLogger.addPseudoDeviceStringVariable(sb, "text", command, millis);
        return DataLogger.closePseudoDeviceEntry(sb, millis);
    }

    static String makeLoginCommandText(String login, String ip, int admin, long millis) {
        StringBuilder sb = DataLogger.startPseudoDeviceEntry(1000);
        DataLogger.addPseudoDeviceStringVariable(sb, "observer", login, millis);
        DataLogger.addPseudoDeviceStringVariable(sb, "ip", ip, millis);
        DataLogger.addPseudoDeviceIntVariable(sb, "admin", admin, millis);
        DataLogger.addPseudoDeviceStringVariable(sb, "text", (admin == 1) ? "(admin)" : "(observer)", millis);
        return DataLogger.closePseudoDeviceEntry(sb, millis);
    }

    void recordHeartbeat(int secs) {
        long time = this.baseTime + ((long) secs) * 1000;
        dm.recordStatus("Heartbeat", time, makeHeartbeatDeviceText(secs % 10 == 0 ? 10 : 1, time));
    }

    public static void recordSDAdvisory(String text) {
        long time =  System.currentTimeMillis();
        WebServer.dm.recordStatus("Comment", time, makeCommentDeviceText("DeviceManager", "127.0.0.0",  text, time));
    }

    void loggerThreadLoop() {
        WebLogObserver obs = WebServer.log.addObserver("<logger thread>");
        int secs = 0;
        int last = 0;

        try {
            while (!Thread.interrupted()) {
                secs = (int) (System.currentTimeMillis() - this.baseTime) / 1000;
                if (secs != last) {
                    recordHeartbeat(secs);
                    last = secs;
                }
                Thread.sleep(200);

                if (writer != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.ensureCapacity(10000);
                    getNewLogEntries(obs, sb);
                    String s = sb.toString();
                    if (s != null && s.length() > 0) {
                        try {
                            write(s);
                        } catch (IOException ex) {
                            System.out.println("DataLogger:write:ioexception: " + ex);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
        }
    }

    static String getNewLogEntryBatch(WebLogObserver obs) {
        StringBuilder sb = new StringBuilder();
        sb.ensureCapacity(10000);
        sb.append("[");

        getNewLogEntries(obs, sb);

        sb.append("{\"status_complete\":\"");
        sb.append(((new Date()).toInstant().toString()));
        sb.append("\"}]");

        return sb.toString();
    }

    static void getNewLogEntries(WebLogObserver obs, StringBuilder sb) {
        ArrayList<WebLogEntry> list = obs.getNewItems();
        if (list == null || list.size() == 0) {
            return;
        }
        if (FusorControlServer.config.superVerbose) {
            //System.out.println("Obs: " + obs + ", updates: " + list.size());
        }

        // Frankly, the client can do this
        // list.sort((a,b)->(int)(a.time-b.time));
        for (WebLogEntry e : list) {
            FusorWebLogEntry fe = (FusorWebLogEntry) e;
            if (isJson(fe.data)) {
                String s = DataLogger.makeLogResponse(fe.device, fe.time, fe.data);
                sb.append(s);
                sb.append(",\n");
            }
        }
    }

    public static boolean isJson(String Json) {
        try {
            new JSONObject(Json);
        } catch (JSONException ex) {
            try {
                new JSONArray(Json);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
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

    private String makeFileName(String ts) {

        return logPath + "fusor-" + ts.replace(":", "-").replace(".", "-");
    }

    private void open(String filePrefix, String ts) throws IOException {
        String fileFullName = filePrefix + ".json";
        currentFile = fileFullName;
        writer = new FileWriter(fileFullName);
        writer.append("{\"base-timestamp\":" + this.baseTime + ",\"instant\":\"" + ts + "\",\"log\":[\n");
        writer.flush();
    }

    private void write(String element) throws IOException {
        writer.append(element);
        writer.flush();
    }

    private void close() throws IOException {
        writer.append("  {}\n]}\n");
        writer.flush();
        writer.close();
        currentFile = "";
    }

    public void makeLogPath() {
        this.logPath = System.getProperty("user.dir") + System.getProperty("file.separator") + "logs";
        if (FusorControlServer.config.logPath != null) {
            this.logPath = FusorControlServer.config.logPath;
        }

        // take off ending slashes for a moment
        if (logPath.endsWith(System.getProperty("file.separator"))) {
            logPath = logPath.substring(0, logPath.length() - 1);
        }
        // make sure log path exists
        createFolder(this.logPath);

        // put the slash back on
        logPath += System.getProperty("file.separator");
        System.out.println("Log path is " + logPath);
        WebServer.logPath = this.logPath;
    }

    public void createFolder(String folder) {
        File file = new File(folder);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                System.err.println("Log folder " + folder + " does not exist and cannot be created");
                System.out.println("Logging to .\\logs instead");

                this.logPath = System.getProperty("user.dir") + System.getProperty("file.separator") + "logs";

                file = new File(this.logPath);
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        System.err.println("Default log folder .\\logs does not exist and cannot be created");
                    }
                }
            }
        }
    }
}
