/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

import static spark.Spark.*;
import com.fazecast.jSerialComm.*;
/**
 *
 * @author paul
 */
public class WebServer {
    
    public WebServer (){
        
    }
    
    public void init() {
        port(8081);
        staticFiles.location("/public");
        get("/", (req, res) -> "<h1><a href='index.html'>Go to index.html</a></h1>");
        get("/kill", (req, res) -> {stop(); System.out.println("Server ended with /kill"); return "server ended";});
    }
}
