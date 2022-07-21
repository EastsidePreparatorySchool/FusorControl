//
// Fusor project code for control Arduino
// "VARIAC"
// needs Adafruit Motor Shield library
//

#include "fusor.h"
#include <Adafruit_MotorShield.h>


#define MINVOLTS 0.0
#define MAXVOLTS 130.0
#define MINSTEPS 0
#define MAXSTEPS 1120

#define STEP_DELAY_MS 50
#define STEPS_TO_ZERO 700

Adafruit_MotorShield AFMS = Adafruit_MotorShield(); 
Adafruit_StepperMotor *myMotor;


int currentVolts = 0.0;

void setup() {
  fusorInit("VARIAC", 100);
  fusorAddVariable("input_volts", FUSOR_VARTYPE_INT);
  fusorAddVariable("dial_volts", FUSOR_VARTYPE_INT);
  fusorAddVariable("stop", FUSOR_VARTYPE_BOOL);

  // stepper control for variac
  AFMS.begin(); 
  myMotor = AFMS.getStepper(400, 1);  // 200 * 2 steps (new gear ratio) per rotation, port 1
//  myMotor->setSpeed(120);              // 60 * 2(new gear ratio) rpm = 1 rps 

  calibrate();
  
  currentVolts = 0;
  fusorSetIntVariable("dial_volts", 0);

  FUSOR_LED_ON();
  fusorDelay(300);
  FUSOR_LED_OFF();
}

int voltToSteps(float volts) {
  return map(volts, MINVOLTS, MAXVOLTS, MINSTEPS, MAXSTEPS);
}

float fmap(int x, float in_min, float in_max, float out_min, float out_max) {
  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}
int stepsToVolts(int steps) {
  return fmap(steps, MINSTEPS, MAXSTEPS, MINVOLTS, MAXVOLTS);
}

void setVoltage(int inputVolts, int speedParam) {
  int diff, steps, sign;
  bool abortSet = false;
  float volts;

  // reset stop detection
  fusorSetBoolVariable("stop", false);

//  myMotor->setSpeed(speedParam);
      

  // convert volts to steps
  if (inputVolts > MAXVOLTS || inputVolts < 0) return;

  steps = voltToSteps(abs(inputVolts - currentVolts));
  sign = ((inputVolts - currentVolts) < 0) ? -1 : 1;

  // do it
  FUSOR_LED_ON();
  int i;
  volts = currentVolts;
  for (i = 0; i<steps; i++) {
    myMotor->step(1, sign>0?FORWARD:BACKWARD, INTERLEAVE); 

    // update our variables
    volts += sign*(float)(stepsToVolts(1000)/1000.f);
    fusorSetIntVariable("dial_volts", volts);
    
    // don't want to do this too fast
    fusorDelay(STEP_DELAY_MS);

    // check for abort
    if (fusorVariableUpdated("stop")) {
      if (fusorGetBoolVariable("stop")) {
        fusorSetBoolVariable("stop", false);
        abortSet = true;
        break;
      }
    }
  }

  if (abortSet) {
    currentVolts = round(volts);
  } else {
    currentVolts = inputVolts;
  }
  fusorSetIntVariable("dial_volts", currentVolts);
  
  // we don't need to hold this by force, turn it off
  myMotor->release();
  FUSOR_LED_OFF();
}

void zeroVoltage() {
  int inputVolts = 0;
  int diff, steps, sign;

  // convert volts to steps
  if (inputVolts > MAXVOLTS || inputVolts < 0) return;

  steps = voltToSteps(abs(inputVolts - currentVolts));
  sign = ((inputVolts - currentVolts) < 0) ? -1 : 1;

  // do it
  FUSOR_LED_ON();
  int i;
  myMotor->step(steps, sign>0?FORWARD:BACKWARD, DOUBLE); 

  // update our variables
  currentVolts = 0;
  fusorSetIntVariable("dial_volts", currentVolts);

  // we don't need to hold this by force, turn it off
  myMotor->release();
  FUSOR_LED_OFF();
}


void extraBackward(int steps, int speedParam) {
  //drive down steps, so we get to actual zero (most situations)
  FUSOR_LED_ON();
//  myMotor->setSpeed(speedParam);
  myMotor->step(steps, BACKWARD, DOUBLE);
//  for (int i=0; i< steps; i++) {
//    myMotor->onestep(BACKWARD, DOUBLE); 
//  }
  myMotor->release();
  FUSOR_LED_OFF();
  fusorSetIntVariable("dial_volts", 0.0);
}

void calibrate() {
  extraBackward(STEPS_TO_ZERO, 255);
  currentVolts = 0;
  fusorSetIntVariable("dial_volts", 0.0);
}

void loop() {
  // must do this in loop, the rest is optional
  fusorLoop();

  updateAll();
  delay(5);
}

void updateAll() {
  int volts;

  // if "input_volts" was updated, set variac to that voltage
  if (fusorVariableUpdated("input_volts")) 
  {
    volts = fusorGetIntVariable("input_volts");
    if (volts == 0) 
    {
      // reasonably quick zero
      zeroVoltage();
      extraBackward(100,255);
    } else if (volts < 0)
    {
      // emergency stop
      calibrate();
      fusorSetIntVariable("input_volts", 0);
    } else 
    {
      // regular set
      setVoltage(volts, 1);
    }
  }
}
