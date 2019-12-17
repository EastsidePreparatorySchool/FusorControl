package com.eastsideprep.fusorcontrolserver;

public class FusorControlServer {

    // debug section
    public static Config config = null;

    private static WebServer serv;

    public static void main(String[] args) {
//        TessWrapper.test();
//        System.exit(0);

        config = Config.readConfig();
        serv = new WebServer();
        serv.init();
    }
}
