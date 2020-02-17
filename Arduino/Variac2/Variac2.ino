//
// Fusor project code for control Arduino
// "VARIAC2"
// Adafruit Feather EPS32 with Motor/Stepper Wing
//

#include "fusor.h"
#include <Adafruit_MotorShield.h>



#define MINVOLTS 0
#define MAXVOLTS 120
#define MINSTEPS 0
#define MAXSTEPS 486

Adafruit_MotorShield AFMS = Adafruit_MotorShield(); 
Adafruit_StepperMotor *myMotor;


int currentVolts = 0;

void setup() {
  fusorInit("VARIAC");
  fusorAddVariable("input_volts", FUSOR_VARTYPE_INT);
  fusorAddVariable("dial_volts", FUSOR_VARTYPE_INT);
  fusorAddVariable("stop", FUSOR_VARTYPE_INT);

  // stepper control for varaic
  AFMS.begin(); 
  myMotor = AFMS.getStepper(200, 1);  // 200 steps per rotation, port 1
  myMotor->setSpeed(60);              // 60 rpm = 1 rps 
  
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

void setVoltage(int volts) {
  int diff, steps, sign;

  if (volts > MAXVOLTS || volts < 0) return;

  steps = voltToSteps(abs(volts - currentVolts));
  sign = ((volts - currentVolts) < 0) ? -1 : 1;

  FUSOR_LED_ON();
  for (int i = 0; i<steps; i++) {
    myMotor->onestep(sign>0?FORWARD:BACKWARD, INTERLEAVE); 

    // update our variables
    if ((i+1)%100 == 0) {
      fusorSetIntVariable("dial_volts", currentVolts + sign*stepsToVolts(i));
      fusorSetIntVariable("input_volts", volts);
      fusorDelayMicroseconds(5);
    }
  }
  // we don't need to hold this by force, turn it off
  myMotor->release();
  FUSOR_LED_OFF();

  currentVolts = volts;
  fusorSetIntVariable("dial_volts", currentVolts);
}



void zeroVoltage() {
  //set the variac as low as we can
  setVoltage(MINVOLTS);

  //drive down 200 bonus steps, so we get to actual zero
  FUSOR_LED_ON();
  for (int i=0; i<200; i++) {
    myMotor->onestep(BACKWARD, SINGLE); 
  }
  myMotor->release();
  FUSOR_LED_OFF();
}

void calibrate() {
  FUSOR_LED_ON();
  for (int i=0; i<500; i++) {
    myMotor->onestep(BACKWARD, DOUBLE); 
  }
  myMotor->release();
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


  // stop (used to be fast, but now isn't
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
      setVoltage(volts);
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
