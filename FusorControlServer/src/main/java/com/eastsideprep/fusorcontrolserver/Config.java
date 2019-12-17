package com.eastsideprep.fusorcontrolserver;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

public class Config {

    public boolean fakeCoreDevices = true;
    public boolean noLog = false;
    public int logFreq = 10;
    public boolean includeCoreStatus = true;
    public boolean verbose = false;
    public boolean superVerbose = false;
    public boolean noBlueTooth = false;
    public String logPath = "logs";
    public long cmdTimeOut = 300;

    private static Gson gson = new Gson();

    public static Config readConfig() {
        String s = null;
        Config config = null;

        try {
            FileReader in = new FileReader("config.json");
            char[] buffer = new char[65000];
            in.read(buffer);
            in.close();
            s = new String(buffer).trim();
        } catch (FileNotFoundException e) {
            try {
                FileWriter out = new FileWriter("config.json");
                s = new Config().toJson();
                out.write(s);
                out.close();
            } catch (Exception e2) {
            }
        } catch (IOException e3) {
        }

        if (s == null) {
            return new Config();
        }

        try {
            //System.out.println("readConfigFile: parsing JSON string: "+config.substring(0, 100) + "...");
            config = gson.fromJson(s, Config.class);
        } catch (JsonSyntaxException e) {
            System.out.println("config:readConfigFile:File parse error");
            System.out.println("config:readConfigFile:     " + e.getMessage() + e.toString());
        } catch (Exception e4) {
            System.out.println("config:readConfigFile:File parse error");
            System.out.println("config:readConfigFile:     " + e4.getMessage() + e4.toString());
        }
        return config;
    }

    public String toJson() {
        return gson.toJson(this, Config.class);
    }
}
