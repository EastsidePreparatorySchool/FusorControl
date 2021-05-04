//
// Fusor project code for control Arduino
// "RP" - Rough pump relay on/off (rotary vane pump
// Arduino Uno
//

#include "fusor.h"

#define RELAY 2 // digital out pin for outlet

int rpstat = false;

void setup(){
  fusorInit("RP", 1000); // longer interval because RP data is not important

  fusorAddVariable("rp_in", FUSOR_VARTYPE_BOOL);
  
  // relay control for solenoid valve
  pinMode(RELAY, OUTPUT);
  digitalWrite(RELAY, LOW);
  //fusorSetBoolVariable("rp_stat", false);
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
    fusorForceUpdate();
  }
}
