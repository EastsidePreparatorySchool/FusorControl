//
// Fusor project code for control Arduino
// "VARIAC"
// Arduino Uno
//

#include "fusor.h"


#define MINVOLTS 0
#define MAXVOLTS 120

#define PUL 4 // stepper motor controller PULSE
#define ENA 3 // stepper motor controller ENABLE
#define DIR 2 // stepper motor controller DIRECTION

#define REL 9  // relay to cut power to controller
#define POT A1 // potentiometer feedback

void setup() {
  fusorInit("VARIAC");
  fusorAddVariable("input_volts", FUSOR_VARTYPE_INT);
  fusorAddVariable("potentiometer", FUSOR_VARTYPE_INT);
  fusorAddVariable("pot_adc", FUSOR_VARTYPE_INT);
  fusorAddVariable("stop", FUSOR_VARTYPE_INT);

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


int potToVolts(int pot) {
  return map(pot, 0, 1024, MINVOLTS, MAXVOLTS);
}

int voltsToPot(int volts) {
  if (volts < MINVOLTS) return 0;
  return map(volts, MINVOLTS, MAXVOLTS, 0, 1024);
}

void setVoltage(int volts, bool emergency) {
  int pot;
  int targetPot = voltsToPot(volts);
  int diff;

  if (volts > MAXVOLTS || volts < 0) return;

  int maxtime = 20000;

  FUSOR_LED_ON();
  digitalWrite(ENA, LOW);
  digitalWrite(REL, HIGH);

  pot = analogRead(POT);
  diff = targetPot - pot;

  long start = millis();
  while (abs(diff) > 5) {
    while (abs(diff) > 5) {
      // make sure do not spend forever in here
      if (millis() - start > maxtime) {
        break;
      }

      // set direction and pulse the stepper
      digitalWrite(DIR, (diff < 0) ? LOW : HIGH);
      digitalWrite(PUL, HIGH);
      delayMicroseconds(200);
      digitalWrite(PUL, LOW);
      delayMicroseconds(200);

      if (emergency) {
        fusorDelayMicroseconds(1);
      } else {
        // try to delay it to something reasonable
        fusorDelayMicroseconds(100);
      }
      // check where we are
      pot = analogRead(POT);
      diff = targetPot - pot;

      if (abs(diff) <= 1)break;

      // update so the client can keep track
      fusorSetIntVariable("pot_adc", pot);
      fusorSetIntVariable("potentiometer", potToVolts(pot));

      if (!emergency) {
        // non-emergency stop
        if (fusorVariableUpdated("stop")) {
          int value = fusorGetIntVariable("stop");
          if (value < 0) {
            // soft stop, say we are done and get out of here
            pot = analogRead(POT);
            volts = potToVolts(pot);
            fusorSetIntVariable("input_volts", volts);
            digitalWrite(ENA, HIGH);
            digitalWrite(REL, LOW);
            FUSOR_LED_OFF(); return;
          } else if (value == 0) {
            // emergency stop and zero fast
            zeroVoltage();
            fusorSetIntVariable("input_volts", 0);
            return;
          }
          // we don't do anything after that.
        }
        // changed min about target?
        if (fusorVariableUpdated("input_volts")) {
          volts = fusorGetIntVariable("input_volts");
          targetPot = voltsToPot(volts);
          diff = targetPot - pot;
        }
      }
      // also play back to client
      fusorSetIntVariable("input_volts", volts);
    }

    // make sure do not spend forever in here
    if (millis() - start > maxtime) {
      break;
    }
    fusorDelay(1000);
    // check where we are
    pot = analogRead(POT);
    diff = targetPot - pot;
  }

  //fusorSendResponse("after loop");
  digitalWrite(ENA, HIGH);
  digitalWrite(REL, LOW);
  FUSOR_LED_OFF();
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
    setVoltage(volts, false); // in non-emergency mode
    if (volts == 0) {
      zeroVoltage();
    }
  }

  // update input volts to get constant status line in display
  // could be done differently, see issue on github
  volts = fusorGetIntVariable("input_volts"); // could have been changed during setVoltage()
  fusorSetIntVariable("input_volts", volts);

  pot = analogRead(POT);
  volts = potToVolts(pot);

  fusorSetIntVariable("pot_adc", pot);
  fusorSetIntVariable("potentiometer", volts);
}
