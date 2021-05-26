//
// Fusor project code for control Arduino
// "VARIAC2"
// Adafruit Feather ESP32 with Motor/Stepper Wing
// Board support https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_dev_index.json
//

#include "fusor.h"
#include <Adafruit_MotorShield.h>



#define MINVOLTS 0.0
#define MAXVOLTS 120.0
#define MINSTEPS 0
#define MAXSTEPS 480

Adafruit_MotorShield AFMS = Adafruit_MotorShield(); 
Adafruit_StepperMotor *myMotor;


float currentVolts = 0.0;

void setup() {
  fusorInit("VARIAC", 1000);
  fusorAddVariable("input_volts", FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("dial_volts", FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("stop", FUSOR_VARTYPE_INT);

  // stepper control for variac
  AFMS.begin(); 
  myMotor = AFMS.getStepper(200, 1);  // 200 steps per rotation, port 1
  myMotor->setSpeed(60);              // 60 rpm = 1 rps 
  
  currentVolts = 0;
  fusorSetFloatVariable("dial_volts", 0.0);

  FUSOR_LED_ON();
  fusorDelay(300);
  FUSOR_LED_OFF();
}

int voltToSteps(float volts) {
  return map(volts, MINVOLTS, MAXVOLTS, MINSTEPS, MAXSTEPS);
}
float stepsToVolts(int steps) {
  return map(steps, MINSTEPS, MAXSTEPS, MINVOLTS, MAXVOLTS);
}

void setVoltage(float volts) {
  int diff, steps, sign;

  if (volts > MAXVOLTS || volts < 0) return;

  steps = voltToSteps(abs(volts - currentVolts));
  sign = ((volts - currentVolts) < 0) ? -1 : 1;

  FUSOR_LED_ON();
  for (int i = 0; i<steps; i++) {
    myMotor->onestep(sign>0?FORWARD:BACKWARD, INTERLEAVE); 

    // update our variables
    if ((i+1)%100 == 0) {
      fusorSetFloatVariable("dial_volts", currentVolts + sign*stepsToVolts(i));
      //fusorSetFloatVariable("input_volts", volts);
      fusorDelayMicroseconds(5);
    }
  }
  // we don't need to hold this by force, turn it off
  myMotor->release();
  FUSOR_LED_OFF();

  currentVolts = volts;
  fusorSetFloatVariable("dial_volts", currentVolts);
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
  float volts;


  // stop (used to be fast, but now isn't)
  if (fusorVariableUpdated("stop")) {
    int value = fusorGetIntVariable("stop");
    if (value == 0) {
      zeroVoltage();
      fusorSetFloatVariable("input_volts", 0.0);
    }
    fusorForceUpdate();
  }

  // if "input_volts" was updated, set variac to that voltage
  if (fusorVariableUpdated("input_volts")) 
  {
    volts = fusorGetFloatVariable("input_volts");
    if (volts == -1) 
    {
      calibrate();
      volts = 0;
    } else 
    {
      setVoltage(volts);
      if (volts == 0) 
      {
        zeroVoltage();
      }
    }
  }
}
