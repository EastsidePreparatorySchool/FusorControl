//
// Fusor project code for control Arduino
// "RP" - Rough pump relay on/off (rotary vane pump
// Arduino Uno
//

#include "fusor.h"

#define RELAY 2 // digital out pin for outlet

int rpstat = false;

void setup(){
  fusorInit("RP");

  fusorAddVariable("rp_in", FUSOR_VARTYPE_BOOL);
  fusorAddVariable("rp_stat", FUSOR_VARTYPE_BOOL);
  
  // relay control for solenoid valve
  pinMode(RELAY, OUTPUT);
  digitalWrite(RELAY, LOW);
  fusorSetBoolVariable("rp_stat", false);
  rpstat = false;

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
  if (fusorVariableUpdated("rp_in")) {
    rpstat = fusorGetBoolVariable("rp_in");
    digitalWrite(RELAY, rpstat?HIGH:LOW);
  }
  fusorSetBoolVariable("rp_stat", rpstat);
}
