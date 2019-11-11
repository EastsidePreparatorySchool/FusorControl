package com.eastsideprep.fusorcontrolserver;

public class FusorControlServer {

    // debug section
    public static boolean fakeCoreDevices = true;
    public static boolean noLog = false;
    public static int logFreq = 10;
    public static boolean includeCoreStatus = true;
    public static boolean verbose = false;
    public static boolean superVerbose = false;

    private static WebServer serv;

    public static void main(String[] args) {
        serv = new WebServer();
        serv.init();
    }
}
