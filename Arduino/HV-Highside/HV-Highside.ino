//
// Fusor project code for HV-highside Arduino
// 
// Adafruit HUZZAH32-ESP32 Feather
// www.adafruit.com
// Board support URL https://dl.espressif.com/dl/package_esp32_index.json
// Bluetooth tutorial: https://randomnerdtutorials.com/esp32-bluetooth-classic-arduino-ide/
// Connect to Windows: https://www.techcoil.com/blog/how-to-connect-to-an-esp32-development-board-via-bluetooth-on-windows-10/
//

#define BLUETOOTH
#include "BluetoothSerial.h"

#include "fusor.h"


FusorVariable fvs[] = {
  //name,    value,  updated
  {"adc",    "",     false},
  {"volts",  "",     false},
  {"amps",   "",     false} 
};


void setup(){
  // light for hope
  pinMode(LED_BUILTIN, OUTPUT);  // pin 13

  fusorInit("HV_HIGHSIDE", fvs, 3); //Fusor device name, variables, num variables
  
  FUSOR_LED_ON();
  delay(1000);
  FUSOR_LED_OFF();
}


void loop() {
  fusorLoop();
  updateAll();
  
  delay(5);
}

void updateAll() {
  // put our variable values out there

  int adc = analogRead(A0); 
  float volts = ((float)adc) * 3.3 / 1024;
  #define OHMS 100
  float amps = (int)(((float)adc) * 3.3 / OHMS / 1024);
  
  fusorSetVariable("adc", NULL, &adc, NULL);
  fusorSetVariable("volts", NULL, NULL, &volts);
  fusorSetVariable("amps", NULL, NULL, &amps);
}
