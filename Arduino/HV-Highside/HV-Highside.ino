//
// Fusor project code for HV-highside Arduino
// 
// Adafruit -ESP32 Feather (HUZZAH32)
// www.adafruit.com
// Board support URL https://dl.espressif.com/dl/package_esp32_index.json
// Bluetooth tutorial: https://randomnerdtutorials.com/esp32-bluetooth-classic-arduino-ide/
// Connect to Windows: https://www.techcoil.com/blog/how-to-connect-to-an-esp32-development-board-via-bluetooth-on-windows-10/
//

#define BLUETOOTH
#include "BluetoothSerial.h"

#include "fusor.h"


void setup(){
  fusorInit("HV_HIGHSIDE"); //Fusor device name, variables, num variables
  fusorAddVariable("adc", FUSOR_VARTYPE_INT);
  fusorAddVariable("volts", FUSOR_VARTYPE_INT);
  fusorAddVariable("amps", FUSOR_VARTYPE_FLOAT);

  if (Serial) {
    Serial.begin(9600);
  }
  FUSOR_LED_ON();
  delay(1000);
  FUSOR_LED_OFF();
}


void loop() {
  while (Serial.available()) {
    Serial.read();
  }
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
  
  fusorSetIntVariable("adc",adc);
  fusorSetIntVariable("volts", volts);
  fusorSetFloatVariable("amps", amps);
}
