//
// Fusor project - PN-junction x-ray sensor code for Arduino
// Adafruit Feather M0
// Adafruit SAMD in board manager

#include "fusor.h"

const int newPercent = 10;

void setup(){
  fusorInit("PN-JUNCTION");

  fusorAddVariable("total", FUSOR_VARTYPE_INT);  
 
  FUSOR_LED_ON();
  delay(200);
  FUSOR_LED_OFF();
}

void loop() {
  fusorLoop();
  
  updateAll();
  fusorDelay(50);
}

void updateAll() {
  static float avgSignal = 0;

  int newSignal = analogRead(A0);
  avgSignal = (avgSignal*(100-newPercent) + newSignal*newPercent)/100.0;

  fusorSetIntVariable("total", (int)avgSignal);
}
