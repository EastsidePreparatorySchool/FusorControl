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
 * @author Administrator
 */
public class FusorControlServer {
    public static void main(String[] args) {
        get("/index.html", (req,res) -> "<html>hello</html>");
    }
}
