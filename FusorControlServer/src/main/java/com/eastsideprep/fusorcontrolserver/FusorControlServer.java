package com.eastsideprep.fusorcontrolserver;


public class FusorControlServer {

    public static Config config = null;

    private static WebServer serv;

    public static void main(String[] args) {
        config = Config.readConfig();
        serv = new WebServer();
        serv.init();
    }
}
