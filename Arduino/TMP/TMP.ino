//
// Fusor project code for control Arduino
// "TMP" - Turbo-Molecular-Pump controller
// Arduino Mega 2560 with custom shield
//

#include "fusor.h"

#define TMP_ON    43 // pin for TMP on(high)/off(low)
#define TMP_SPEED 42 // pin for TMP speed low/high
#define TMP_AMPS  A0 // pin for pump amps 0-10V = 0 - 2.5A
#define TMP_FREQ  A1 // pin for pump freq 0-10V = 0 - 1250Hz

FusorVariable fvs[] = {
  //name,   value,  updated
  {"TMP",   "off",  false},
  {"SPEED", "high", false},
  {"FREQ",  "",     false},
  {"AMPS",  "",     false}
};




void setup(){
  // light for hope
  pinMode(LED_BUILTIN, OUTPUT);  // pin 13

  // must do this in init, the rest is optional
  fusorInit("TMP", fvs, 4);
  
  FUSOR_LED_ON();
  delay(200);
  FUSOR_LED_OFF();
}

void loop() {
  // must do this in loop, the rest is optional
  fusorLoop();
  
  updateAll();
  delay(5);
}


void updateAll() {
  // put our current amps and freq out
  int amps = analogRead(TMP_AMPS);
  int freq = analogRead(TMP_FREQ);
  fusorSetVariable("AMPS", NULL, &amps, NULL);
  fusorSetVariable("FREQ", NULL, &freq, NULL);


  // if "TMP" was updated, read it switch the pump on/off accordingly
  if (fusorVariableUpdated("TMP")) {
    if (fusorStrVariableEquals("TMP", "ON")) {
      tmpOn();
    } else if (fusorStrVariableEquals("TMP", "OFF")) {
      tmpOff();
    }
  }

  // if "SPEED" was updated, read it switch the pump to high speed / low speed accordingly
  if (fusorVariableUpdated("SPEED")) {
    if (fusorStrVariableEquals("SPEED", "LOW")) {
      tmpLow();
    } else if (fusorStrVariableEquals("SPEED", "HIGH")) {
      tmpHigh();
    }
  }
}

void tmpOn() {
  FUSOR_LED_ON();
  digitalWrite(TMP_ON, LOW);
  delay(100);
  FUSOR_LED_OFF();
}

void tmpOff() {
  FUSOR_LED_ON();
  digitalWrite(TMP_ON, HIGH);
  delay(100);
  FUSOR_LED_OFF();
}

void tmpLow() {
  FUSOR_LED_ON();
  digitalWrite(TMP_SPEED, LOW);
  delay(100);
  FUSOR_LED_OFF();
}

void tmpHigh() {
  FUSOR_LED_ON();
  digitalWrite(TMP_SPEED, HIGH);
  delay(100);
  FUSOR_LED_OFF();
}
