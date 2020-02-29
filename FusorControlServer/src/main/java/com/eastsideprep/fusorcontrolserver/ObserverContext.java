/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

import static com.eastsideprep.fusorcontrolserver.WebServer.cd;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;
import static spark.Spark.halt;

/**
 *
 * @author gmein
 */
public class ObserverContext extends Context {

    ObserverContext(String login, WebServer ws) {
        super(login, ws);
    }

    String getStatusRoute() {
        //System.out.println("/getstatus");
        if (WebServer.dl == null) {
            return "not logging";
        }

        String s = DataLogger.getNewLogEntryBatch(obs);

        if (FusorControlServer.config.superVerbose) {
            //System.out.println("  Status:" + s);
        }
        return s;
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

        if (cd.variac.set("stop", 0)) {
            System.out.println("variac 0");
            return "emergency stop complete";
        } else {
            throw halt("Variac stop failed");
        }
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

    private String getURL(String url) {
        url = url.replace(" ", "%20");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, BodyHandlers.ofString());
        } catch (Exception ex) {
            System.out.println("Exc in getting file from souce: " + ex);
        }

        return response.body();
    }

    private ArrayList<String> getGithubFusorLogs() {
        String s = getURL("https://github.com/EastsidePreparatorySchool/FusorExperiments/tree/master/logs/keep");
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
            String s = getURL(filename);
            HttpServletResponse raw = res.raw();
            raw.getOutputStream().print(s);
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
