/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eastsideprep.fusorcontrolserver;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 *
 * @author thaeger
 */
public class CSVRecorder {
    FileWriter csvWriter;
    
    public void init(List<String> headers) throws IOException {
        //creates time stamp and appends to csv file, then creates a writer and inits headers
        Date date = new Date();
        Instant instant1 = date.toInstant();
        String ts = instant1.toString().replace(":", " ").replace(".", " ");
        String fileName = "test " + ts + ".csv";
        //makes inits fmile and Writer
        csvWriter = new FileWriter(fileName);
        csvWriter.append(String.join(",", headers));
        csvWriter.append("\n");
    }
    
    public void write(List<String> row) throws IOException {
        csvWriter.append(String.join(",", row));
        csvWriter.append("\n");
    }
    
    public void close() throws IOException {
        csvWriter.flush();
        csvWriter.close();
    }
}