package com.eastsideprep.fusorcontrolserver;

public class FusorControlServer {

    public static boolean debug = true;

    private static WebServer serv;

    public static void main(String[] args) {
        serv = new WebServer();
        serv.init();
    }
}
