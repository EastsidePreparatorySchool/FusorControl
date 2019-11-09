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

void setup(){
  fusorInit("TMP");
  fusorAddVariable("tmp", FUSOR_VARTYPE_BOOL);
  fusorAddVariable("lowspeed", FUSOR_VARTYPE_BOOL);
  fusorAddVariable("freq", FUSOR_VARTYPE_INT);
  fusorAddVariable("amps", FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("amps_adc", FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("freq_adc", FUSOR_VARTYPE_FLOAT);

  fusorSetBoolVariable("tmp", false);
  fusorSetBoolVariable("lowspeed", false);
  fusorSetIntVariable("freq", 0);
  fusorSetIntVariable("amps_adc", 0);
  fusorSetFloatVariable("amps", 0.0);

  tmpOff();
  tmpLow();
  
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
  fusorSendResponse("updating ...");
  // put our current amps and freq out
  int amps = analogRead(TMP_AMPS);
  int freq = analogRead(TMP_FREQ);
  fusorSetIntVariable("amps_adc", amps);
  fusorSetIntVariable("freq_adc", freq);
  fusorSetFloatVariable("amps", (float)amps); // TODO: convert
  fusorSetFloatVariable("freq", (float)freq); // TODO: convert

  fusorSendResponse("done setting reads ...");

  // if "tmp" was updated, read it switch the pump on/off accordingly
  if (fusorVariableUpdated("tmp")) {
    if (fusorGetBoolVariable("tmp")) {
      tmpOn();
      fusorSetBoolVariable("tmp", true);
    } else {
      tmpOff();
      fusorSetBoolVariable("tmp", false);
    }
  }

  fusorSendResponse("done processing tmp sets ...");


  // if "lowspeed" was updated, read it switch the pump to high speed / low speed accordingly
  if (fusorVariableUpdated("speed")) {
    if (fusorGetBoolVariable("lowspeed")) {
      tmpLow();
      fusorSetBoolVariable("lowspeed", true);
    } else {
      tmpHigh();
      fusorSetBoolVariable("lowspeed", false);
    }
  }
  fusorSendResponse("done updating");

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
