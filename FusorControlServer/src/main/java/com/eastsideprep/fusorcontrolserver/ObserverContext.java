package com.eastsideprep.fusorcontrolserver;

import static com.eastsideprep.fusorcontrolserver.WebServer.cd;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;
import static spark.Spark.halt;

public class ObserverContext extends Context {

    ObserverContext(String login, WebServer ws) {
        super(login, ws);
    }

    String getStatusRoute() {
        try {
            //System.out.println("/getstatus");
            if (WebServer.dl == null) {
                return "not logging";
            }

            String s = DataLogger.getNewLogEntryBatch(obs);

            if (FusorControlServer.config.superVerbose) {
                //System.out.println("  Status:" + s);
            }
            return s;
        } catch (Throwable t) {
            System.out.println("");
            System.out.println("Exception in getStatus:" + t.getMessage());
            StackTraceElement[] aste = t.getStackTrace();
            for (int i = 0; i < aste.length; i++) {
                System.out.println(aste[i].toString());
            }
            System.out.println("exception in getStatus");
            return "exception";
        }
    }

    String comment(spark.Request req) {
        String text = req.queryParams("text");
        String ip = req.ip();

        long millis = System.currentTimeMillis();
        String logText = DataLogger.makeCommentDeviceText(this.name, ip, text, millis);

        System.out.println("Comment: " + logText);
        WebServer.dm.recordStatus("Comment", millis, logText);

        return "ok";
    }

    String emergencyStop(spark.Request req) {
        System.out.println("Received Emergency Stop ");
        long millis = System.currentTimeMillis();
        String logText = DataLogger.makeEmergencyStopDeviceText(this.name, ip, millis);
        WebServer.dm.recordStatus("Command", millis, logText);

        cd.gas.setClosed();
        System.out.println("Closed solenoid");
        
        cd.gas.set("nv_in", 0);
        System.out.println("Closed needle valve");
        
        cd.variac.set("input", 0);
        System.out.println("variac 0");

        cd.hvrelay.off();
        System.out.println("hv relay off");

        return "emergency stop attempt complete";
    }

    private static ArrayList<String> match(String text, String pattern) {
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(text);

        ArrayList<String> result = new ArrayList<>();
        while (m.find()) {
            String s = m.group();
            result.add(s.substring(s.lastIndexOf("/") + 1));
        }

        return result;
    }

    private byte[] getURL(String url) {
        url = url.replace(" ", "%20");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<byte[]> response = null;
        try {
            response = client.send(request, BodyHandlers.ofByteArray());
        } catch (Exception ex) {
            System.out.println("Exc in getting file from souce: " + ex);
        }

        return response.body();
    }

    private ArrayList<String> getGithubFusorLogs() {
        String s = new String(getURL("https://github.com/EastsidePreparatorySchool/FusorExperiments/tree/master/logs/keep"));
        return match(s, "\\/EastsidePreparatorySchool\\/FusorExperiments\\/blob\\/master\\/logs\\/keep\\/fusor-[^\\. ]*\\.json");
    }

    ArrayList<String> getLogFileNames() {
        ArrayList<String> result = new ArrayList<>();
        result = getGithubFusorLogs();
        return result;
    }

    HttpServletResponse getLogFile(spark.Request req, spark.Response res) {
        String filename = req.queryParams("filename");
        if (filename == null || filename.equals("")) {
            System.out.println("getLogFile got no filename");
            return null;
        }

        System.out.println("logfile requested: " + filename);
        try {
            filename = "https://raw.githubusercontent.com/EastsidePreparatorySchool/FusorExperiments/master/logs/keep/" + filename;
            byte[] ab = getURL(filename);
            HttpServletResponse raw = res.raw();
            raw.getOutputStream().write(ab);
            raw.getOutputStream().flush();
            raw.getOutputStream().close();
            res.header("Content-Type", "application/JSON");
            return raw;
        } catch (Exception e) {
            System.out.println("getLogFile: Exception: " + e);
            return null;
        }
    }

}
