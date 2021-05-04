//
// Fusor project code for control Arduino
// "HV-RELAY" - Variac relay on/off
// Arduino Uno
//

#include "fusor.h"

#define RELAY 2 // digital out pin for outlet

int stat = false;

void setup(){
  fusorInit("HV-RELAY", 1000); // longer interval because RP data is not important

  fusorAddVariable("in", FUSOR_VARTYPE_BOOL);
  
  // relay control 
  pinMode(RELAY, OUTPUT);
  digitalWrite(RELAY, LOW);
  stat = false;

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
  if (fusorVariableUpdated("in")) {
    stat = fusorGetBoolVariable("in");
    digitalWrite(RELAY, stat?HIGH:LOW);
    fusorForceUpdate();
  }
}
