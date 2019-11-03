package com.eastsideprep.fusorcontrolserver;

public class FusorControlServer {

    static WebServer serv;
    
    public static void main(String[] args) {
        serv = new WebServer();
        serv.init();
    }
}
