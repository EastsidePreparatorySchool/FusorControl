//
// Fusor project code for control Arduino
// "GAS" - gas controller
// Adafruit Feather Huzzah ESP8266 Wifi 
// Board support: http://arduino.esp8266.com/stable/package_esp8266com_index.json
//

#include "fusor.h"

#define SOL 15 // digital out pin for solenoid

void setup(){
  fusorInit("GAS");
  fusorAddVariable("solenoid", FUSOR_VARTYPE_BOOL);
  fusorSetBoolVariable("solenoid", false);

  // relay control for solenoid valve
  pinMode(SOL, OUTPUT);
  digitalWrite(SOL, LOW);

  FUSOR_LED_ON();
  delay(300);
  FUSOR_LED_OFF();
}

void loop() {
  fusorLoop();
  updateAll();
  delay(5);
}

void updateAll() {
  // if "solenoid" was updated, open or close it
  if (fusorVariableUpdated("solenoid")) {
    digitalWrite(SOL, fusorGetBoolVariable("solenoid")?HIGH:LOW);
  }
}
