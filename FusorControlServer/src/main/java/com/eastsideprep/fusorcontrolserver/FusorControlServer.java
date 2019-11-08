package com.eastsideprep.fusorcontrolserver;

public class FusorControlServer {

    // debug section
    public static boolean fakeCoreDevices = true;
    public static boolean noLog = false;
    public static boolean includeCoreStatus = true;
    public static boolean verbose = true;

    private static WebServer serv;

    public static void main(String[] args) {
        serv = new WebServer();
        serv.init();
    }
}
