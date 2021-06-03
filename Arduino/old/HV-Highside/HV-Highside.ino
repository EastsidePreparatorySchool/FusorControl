//
// Fusor project code for HV-highside Arduino
// 
// Adafruit -ESP32 Feather (HUZZAH32)
// www.adafruit.com
// Board support URL https://dl.espressif.com/dl/package_esp32_index.json (11/12: offline)
// OLED display tutorial for WeMos Lolin with display: https://randomnerdtutorials.com/esp32-built-in-oled-ssd1306/
// Bluetooth tutorial: https://randomnerdtutorials.com/esp32-bluetooth-classic-arduino-ide/
// Connect to Windows: https://www.techcoil.com/blog/how-to-connect-to-an-esp32-development-board-via-bluetooth-on-windows-10/
//

#include <BluetoothSerial.h>
#define BLUETOOTH
#include "fusor.h"

#define OHMS 100  // highside current measurement resistor size

void setup(){
  fusorInit("HV-HIGHSIDE"); //Fusor device name, variables, num variables
  fusorAddVariable("hs_current_adc", FUSOR_VARTYPE_INT);
  fusorAddVariable("hs_current_amps", FUSOR_VARTYPE_FLOAT);

  // service the serial port in a vain attempt to get this working when it is connected both ways
  if (Serial) {
    Serial.begin(9600);
  }
  FUSOR_LED_ON();
  delay(200);
  FUSOR_LED_OFF();
}


void loop() {
  // drain the serial port
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
  float amps = (int)(((float)adc) * 3.3 / OHMS / 1024);
  
  fusorSetIntVariable("hs_current_adc",adc);
  fusorSetFloatVariable("hs_current_amps", amps);
}
