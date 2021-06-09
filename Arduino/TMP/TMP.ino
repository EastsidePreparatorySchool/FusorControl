//
// Fusor project code for control Arduino
// "TMP" - Turbo-Molecular-Pump controller
// Sparkfun Uno with breadboard shield
//

#include "fusor.h"

#define TMP_ON    2  // pin for TMP on(high)/off(low)
#define TMP_SPEED 3  // pin for TMP speed low/high
//#define TMP_ERROR 4  // pin will have "signal present" for errors
#define TMP_ERROR 2  // temporary: analog pin will have "signal present" for errors
#define TMP_RESET 5  // ground this pin to reset error condition
#define TMP_AMPS  A0 // pin for pump amps 0-10V = 0 - 2.5A
#define TMP_FREQ  A1 // pin for pump freq 0-10V = 0 - 1250Hz

void setup() {
  fusorInit("TMP");
  fusorAddVariable("tmp", FUSOR_VARTYPE_BOOL);
  fusorAddVariable("lowspeed", FUSOR_VARTYPE_BOOL);
  fusorAddVariable("error", FUSOR_VARTYPE_INT);
  fusorAddVariable("reset", FUSOR_VARTYPE_BOOL);
  
  fusorAddVariable("pump_curr_amps", FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("pump_freq", FUSOR_VARTYPE_FLOAT);

  fusorSetBoolVariable("tmp", false);  
  fusorSetBoolVariable("error", false);  
  fusorSetBoolVariable("reset", false);  
  fusorSetBoolVariable("lowspeed", false);
  fusorSetFloatVariable("pump_curr_amps", 0.0);
  fusorSetFloatVariable("pump_freq", 0.0);

  pinMode(TMP_ON, OUTPUT);
  pinMode(TMP_SPEED, OUTPUT);
  pinMode(TMP_RESET, OUTPUT);
  //pinMode(TMP_ERROR, INPUT);
  

  tmpOff();
  tmpHigh();

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
  //fusorSendResponse("updating ...");
  // put our current amps and freq out, read them a few times before counting the result

  //for (int i = 0; i < 10; analogRead(TMP_AMPS), i++);
  int amps = analogRead(TMP_AMPS);

  //for (int i = 0; i < 10; analogRead(TMP_FREQ), i++);
  int freq = analogRead(TMP_FREQ);

  fusorSetFloatVariable("pump_curr_amps", ((float)amps) * 2.5f / 1024.0f); // full adc 1024 = 2.5A
  fusorSetFloatVariable("pump_freq", ((float)freq) * 1250.0f / 1024.0f); // full adc 1024 = 1250 Hz
  fusorSetBoolVariable("error", tmpError());

  //fusorSendResponse("done setting reads ...");

  // if "tmp" was updated, read it switch the pump on/off accordingly
  if (fusorVariableUpdated("tmp")) {
    if (fusorGetBoolVariable("tmp")) {
      tmpOn();
    } else {
      tmpOff();
    }
  }
 

  //fusorSendResponse("done processing tmp sets ...");


  // if "reset" was updated, execute reset
  if (fusorVariableUpdated("reset")) {
    if (fusorGetBoolVariable("reset")) {
      tmpReset();
      fusorSetBoolVariable("reset", false);
    }
  }

  // if "lowspeed" was updated, read it, switch the pump to high speed / low speed accordingly
  if (fusorVariableUpdated("lowspeed")) {
    if (fusorGetBoolVariable("lowspeed")) {
      tmpLow();
    } else {
      tmpHigh();
    }
  }
  //fusorSendResponse("done updating");
}

void tmpOn() {
  FUSOR_LED_ON();
  digitalWrite(TMP_ON, HIGH);
  //delay(100);
  FUSOR_LED_OFF();
}

void tmpOff() {
  FUSOR_LED_ON();
  digitalWrite(TMP_ON, LOW);
  //delay(100);
  FUSOR_LED_OFF();
}

void tmpLow() {
  FUSOR_LED_ON();
  digitalWrite(TMP_SPEED, LOW);
  //delay(100);
  FUSOR_LED_OFF();
}

void tmpHigh() {
  FUSOR_LED_ON();
  digitalWrite(TMP_SPEED, HIGH);
  //delay(100);
  FUSOR_LED_OFF();
}


//bool tmpError() {
int tmpError() {
  //return digitalRead(TMP_ERROR) == HIGH;
  return analogRead(TMP_ERROR);
}

void tmpReset() {
  FUSOR_LED_ON();
  digitalWrite(TMP_RESET, LOW);
  delay(1200);
  digitalWrite(TMP_RESET, HIGH);
  FUSOR_LED_OFF();
}
