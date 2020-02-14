//
// Fusor project code for control Arduino
// "VARIAC"
// Arduino Uno
//

#include "fusor.h"


#define MINVOLTS 0
#define MAXVOLTS 120
#define MINSTEPS 0
#define MAXSTEPS 3830

#define PUL 4 // stepper motor controller PULSE
#define ENA 3 // stepper motor controller ENABLE
#define DIR 2 // stepper motor controller DIRECTION

#define REL 9  // relay to cut power to controller

int currentVolts = 0;

void setup() {
  fusorInit("VARIAC");
  fusorAddVariable("input_volts", FUSOR_VARTYPE_INT);
  fusorAddVariable("dial_volts", FUSOR_VARTYPE_INT);
  fusorAddVariable("stop", FUSOR_VARTYPE_INT);

  // stepper control for varaic
  pinMode(PUL, OUTPUT);
  pinMode(ENA, OUTPUT);
  pinMode(DIR, OUTPUT);
  pinMode(REL, OUTPUT);

  // stepper controller power relay
  digitalWrite(REL, LOW);

  currentVolts = 0;
  fusorSetIntVariable("dial_volts", 0);
  FUSOR_LED_ON();
  fusorDelay(300);
  FUSOR_LED_OFF();
}

int voltToSteps(int volts) {
  return map(volts, MINVOLTS, MAXVOLTS, MINSTEPS, MAXSTEPS);
}
int stepsToVolts(int steps) {
  return map(steps, MINSTEPS, MAXSTEPS, MINVOLTS, MAXVOLTS);
}

void setVoltage(int volts, bool emergency) {
  int diff, steps, sign;

  if (volts > MAXVOLTS || volts < 0) return;

  steps = voltToSteps(abs(volts - currentVolts));
  digitalWrite(DIR, ((volts - currentVolts) < 0) ? LOW : HIGH);
  sign = ((volts - currentVolts) < 0) ? -1 : 1;

  FUSOR_LED_ON();
  digitalWrite(ENA, LOW);
  digitalWrite(REL, HIGH);
  for (int i = 0; i<steps; i++) {
    digitalWrite(PUL, HIGH);
    delayMicroseconds(200);
    digitalWrite(PUL, LOW);
    delayMicroseconds(200);
    
    if (!emergency){
      // try to delay it to something reasonable
      fusorDelayMicroseconds(5700);
    
      if ((i+1)%100 == 0) {
        fusorSetIntVariable("dial_volts", currentVolts + sign*stepsToVolts(i));
        fusorSetIntVariable("input_volts", volts);
      }
    }
  }
  digitalWrite(ENA, HIGH);
  digitalWrite(REL, LOW);
  FUSOR_LED_OFF();

  currentVolts = volts;
  fusorSetIntVariable("dial_volts", currentVolts);
}



void zeroVoltage() {
  //set the variac as low as we can
  setVoltage(MINVOLTS, true);

  //drive down some bonus steps, so we get to actual zero
  FUSOR_LED_ON();
  digitalWrite(ENA, LOW);
  digitalWrite(REL, HIGH);

  digitalWrite(DIR, LOW);
  for (int i = 0; i < 200; i++) {
    digitalWrite(PUL, HIGH);
    delayMicroseconds(400);
    digitalWrite(PUL, LOW);
    delayMicroseconds(400);
    fusorDelayMicroseconds(5);
  }
  digitalWrite(ENA, HIGH);
  digitalWrite(REL, LOW);
  FUSOR_LED_OFF();
}

void calibrate() {
  FUSOR_LED_ON();
  digitalWrite(ENA, LOW);
  digitalWrite(REL, HIGH);

  digitalWrite(DIR, LOW);
  for (int i = 0; i < 4000; i++) {
    digitalWrite(PUL, HIGH);
    delayMicroseconds(400);
    digitalWrite(PUL, LOW);
    delayMicroseconds(400);
    fusorDelayMicroseconds(5);
  }
  digitalWrite(ENA, HIGH);
  digitalWrite(REL, LOW);
  FUSOR_LED_OFF();
  currentVolts = 0;
}

void loop() {
  // must do this in loop, the rest is optional
  fusorLoop();

  updateAll();
  delay(5);
}

void updateAll() {
  int volts;


  // emergency shutdown
  if (fusorVariableUpdated("stop")) {
    int value = fusorGetIntVariable("stop");
    if (value == 0) {
      zeroVoltage();
      fusorSetIntVariable("input_volts", 0);
    }
  }

  // if "input_volts" was updated, set variac to that voltage
  if (fusorVariableUpdated("input_volts")) {
    volts = fusorGetIntVariable("input_volts");
    if (volts == -1) {
      calibrate();
      volts = 0;
    } else {
      setVoltage(volts, false); // in non-emergency mode
      if (volts == 0) {
        zeroVoltage();
    }
    }
  }

  // update input volts to get constant status line in display
  // could be done differently, see issue on github
  volts = fusorGetIntVariable("input_volts"); // could have been changed during setVoltage()
  volts = max(volts, 0);
  fusorSetIntVariable("input_volts", volts);
  fusorSetIntVariable("dial_volts", currentVolts);
}
