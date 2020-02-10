//
// Fusor project - Diaphragm pressure sensor code for Arduino
// Arduino Uno
// 
//

#include "fusor.h"


long avgSignal;
int newPercent = 10;

void setup(){
  fusorInit("DIAPHRAGM");
  
  // fixed analog input
  fusorAddVariable("diaphragm_adc", FUSOR_VARTYPE_INT);
  avgSignal = analogRead(A0);

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
  long newSignal = analogRead(A0);
  avgSignal = (avgSignal*(100-newPercent) + newSignal*newPercent)/100;
  
  fusorSetIntVariable("diaphragm_adc", (int)avgSignal);
}
  
  
 
