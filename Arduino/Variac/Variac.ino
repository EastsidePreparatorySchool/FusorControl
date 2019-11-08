//
// Fusor project code for control Arduino
// "VARIAC"
// Arduino Uno
//

#include "fusor.h"


#define MINVOLTS 5
#define MAXVOLTS 120

#define PUL 35 // stepper motor controller PULSE
#define ENA 37 // stepper motor controller ENABLE
#define DIR 36 // stepper motor controller DIRECTION

#define REL 9  // relay to cut power to controller
#define POT A2 // potentiometer feedback

#define delayMicros 1000



FusorVariable fvs[] = {
  //name,           value,  updated
  {"volts",         "-1",   false},
  {"potentiometer", "-1",   false} 
};


void setup(){
  fusorInit("VARIAC", fvs, 2);

  // stepper control for varaic
  pinMode(PUL, OUTPUT);
  pinMode(ENA, OUTPUT);
  pinMode(DIR, OUTPUT);

  // stepper controller power relay
  pinMode(REL, OUTPUT);

  // feedback from variac
  pinMode(POT, INPUT);

  //zeroVoltage();
  FUSOR_LED_ON();
  delay(300);
  FUSOR_LED_OFF();
}


float potToVolts(int pot) {
  return map(pot, 0, 1024, MINVOLTS, MAXVOLTS);
}

int voltsToPot(int volts) {
  if (volts < MINVOLTS) return 0;
  return map(volts, MINVOLTS, MAXVOLTS, 0, 1024);
}

void setVoltage(int volts) {
  int pot;
  int targetPot = voltsToPot(volts);
  if (volts > 90) return;
  int dif;

  FUSOR_LED_ON();
  digitalWrite(ENA, LOW);
  digitalWrite(REL, HIGH);

  long start = millis();
  do {
    // make sure do not spend forever in here
    if (millis - start > 3000) {
      break;
    }
    pot = analogRead(POT);
    dif = targetPot - pot;
    digitalWrite(DIR, (dif < 0) ? LOW : HIGH);

    digitalWrite(PUL, HIGH);
    delayMicroseconds(delayMicros);
    digitalWrite(PUL, LOW);
    delayMicroseconds(delayMicros);
  } while ( abs(dif) > 10 );

  digitalWrite(ENA, HIGH);
  digitalWrite(REL, LOW);
  FUSOR_LED_OFF();
}

void zeroVoltage() {
  //set the variac as low as we can
  setVoltage(MINVOLTS);

  //drive down some bonus steps, so we get to actual zero
  FUSOR_LED_ON();
  digitalWrite(ENA, LOW);
  digitalWrite(REL, HIGH);

  digitalWrite(DIR, LOW);
  for (int i = 0; i < 20; i++) {
    digitalWrite(PUL, HIGH);
    delayMicroseconds(delayMicros * 2);
    digitalWrite(PUL, LOW);
    delayMicroseconds(delayMicros * 2);
  }
  digitalWrite(ENA, HIGH);
  digitalWrite(REL, LOW);
  FUSOR_LED_OFF();
}


void loop() {
  // must do this in loop, the rest is optional
  fusorLoop();
  
  updateAll();
  delay(5);
}

void updateAll() {
  // put our current potentiometer reading into "potentiometer"
  int pot = analogRead(POT);
  int volts = potToVolts(pot);

  fusorSetVariable("potentiometer", NULL, &pot, NULL);

  // if "volts" was updated, set variac to that voltage
  if (fusorVariableUpdated("volts")) {
    volts = fusorGetIntVariable("volts");
    setVoltage(volts);
  }
  
  fusorSetVariable("volts", NULL, &volts, NULL);
}
