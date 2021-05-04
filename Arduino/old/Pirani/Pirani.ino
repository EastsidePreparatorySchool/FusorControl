//
// Fusor project - Pirani gauge pressure sensor code for Arduino
// Arduino Uno
//

#include "fusor.h"

long avgSignal;
int newPercent = 10;

void setup(){
  fusorInit("PIRANI");
  
  // fixed analog input
  fusorAddVariable("pirani_adc", FUSOR_VARTYPE_INT);
  fusorSetIntVariable("pirani_adc", 0);
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

  fusorSetIntVariable("pirani_adc", (int) avgSignal);
}
  
  
 
