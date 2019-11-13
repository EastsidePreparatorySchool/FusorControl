/*
  SD card datalogger

 This example shows how to log data from three analog sensors
 to an SD card using the SD library.

 The circuit:
 * analog sensors on analog ins 0, 1, and 2
 * SD card attached to SPI bus as follows:
 ** MOSI - pin 11
 ** MISO - pin 12
 ** CLK - pin 13
 ** CS - pin 4 (for MKRZero SD: SDCARD_SS_PIN)

 created  24 Nov 2010
 modified 9 Apr 2012
 by Tom Igoe

 This example code is in the public domain.

 */

const int in1 = A0;
const int in2 = A2; //blue wire (second from right) goes to A2, all others go to ground
int timer = 0;
int time2 = 0;

#include <SPI.h>
#include <SD.h>

const int chipSelect = 4;

void setup() {
  timer = 0;
  time2 = 0;
  // Open serial communications and wait for port to open:
  Serial.begin(9600);
  while (!Serial) {
    // wait for serial port to connect. Needed for native USB port only
  }
  Serial.print("Writing Data ");
  Serial.print("Initializing SD card...");

  // see if the card is present and can be initialized:
  if (!SD.begin(chipSelect)) {
    Serial.println("Card failed, or not present");
    // don't do anything more:
    while (1);
  }
   Serial.println("card initialized.");
  /*if(SD.exists("data.txt")){
    SD.remove("data.txt");
  }*/
}

void loop() {
  // make a string for assembling the data to log:
  String dataString = "";

  // read three sensors and append to the string:
  int ain = analogRead(in2);
  dataString += String(ain) + ", " + String(time2);

  // open the file. note that only one file can be open at a time,
  // so you have to close this one before opening another.
  File dataFile = SD.open("data.txt", FILE_WRITE);

  // if the file is available, write to it:
  if (dataFile) {
    if(timer < 1){
        for(int i = 0; i<10; i++){
         dataFile.println(String(i));
         delay(10);
       }
       timer += 1;
    }
    if(ain > 90){
      dataFile.println(dataString);
      Serial.println(dataString);
    }
    dataFile.close();
  } else { // if the file isn't open, pop up an error:
    Serial.println("error opening testlog.txt");
    dataFile.close();
  }
  time2++;
  delay(100);
  if(timer > 400){  
    while(1);
  }
}
