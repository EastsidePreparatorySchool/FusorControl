package com.eastsideprep.fusorcontrolserver;

import com.eastsideprep.cameras.TessWrapper;

public class FusorControlServer {

    // debug section
    public static Config config = null;

    private static WebServer serv;

    public static void main(String[] args) {
//        TessWrapper.bytespp = 4;
//        TessWrapper.test();
//        System.exit(0);

        config = Config.readConfig();
        serv = new WebServer();
        serv.init();
    }
}
