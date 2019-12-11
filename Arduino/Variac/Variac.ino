//
// Fusor project code for control Arduino
// "VARIAC"
// Arduino Uno
//

#include "fusor.h"


#define MINVOLTS 5
#define MAXVOLTS 120

#define PUL 4 // stepper motor controller PULSE
#define ENA 3 // stepper motor controller ENABLE
#define DIR 2 // stepper motor controller DIRECTION

#define REL 9  // relay to cut power to controller
#define POT A1 // potentiometer feedback

void setup(){
  fusorInit("VARIAC");
  fusorAddVariable("input_volts", FUSOR_VARTYPE_INT);
  fusorAddVariable("potentiometer", FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("pot_adc", FUSOR_VARTYPE_INT);

  // stepper control for varaic
  pinMode(PUL, OUTPUT);
  pinMode(ENA, OUTPUT);
  pinMode(DIR, OUTPUT);

  // stepper controller power relay
  pinMode(REL, OUTPUT);
  digitalWrite(REL, LOW);

  // feedback from variac
  pinMode(POT, INPUT);

  //zeroVoltage();
  FUSOR_LED_ON();
  fusorDelay(300);
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
  if (volts > MAXVOLTS) return;
  int dif;

  FUSOR_LED_ON();
  digitalWrite(ENA, LOW);
  digitalWrite(REL, HIGH);

  //fusorSendResponse("before");
  long start = millis();
  do {
    // make sure do not spend forever in here
    if (millis() - start > 3000) {
      break;
    }
    //fusorSendResponse("in loop2");
    pot = analogRead(POT);
    dif = targetPot - pot;
    digitalWrite(DIR, (dif < 0) ? LOW : HIGH);

    digitalWrite(PUL, HIGH);
    delayMicroseconds(200);
    digitalWrite(PUL, LOW);
    delayMicroseconds(200);
    //fusorSendResponse("at end");
    fusorDelayMicroseconds(5); // just to give it a chance to drain the USB queue and provide status
  } while ( abs(dif) > 10 );

  //fusorSendResponse("after loop");
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
    delayMicroseconds(400);
    digitalWrite(PUL, LOW);
    delayMicroseconds(400);
    fusorDelayMicroseconds(5);
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
  int pot;
  int volts; 
  
  // put our current potentiometer reading into "potentiometer"

  // if "input_volts" was updated, set variac to that voltage
  if (fusorVariableUpdated("input_volts")) {
    volts = fusorGetIntVariable("input_volts");
    setVoltage(volts);
  }
  
  pot = analogRead(POT);
  volts = potToVolts(pot);

  fusorSetIntVariable("pot_adc", pot);
  fusorSetFloatVariable("potentiometer", ((float)pot)*5.0f/1024);
  fusorSetIntVariable("input_volts", volts);
}
