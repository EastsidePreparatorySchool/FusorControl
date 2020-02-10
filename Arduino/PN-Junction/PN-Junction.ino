//
// Fusor project - PN-junction x-ray sensor code for Arduino
//
// #define BLUETOOTH

#include "fusor.h"

long avgSignal = 0;
int newPercent = 10;

void setup(){
  fusorInit("PN-JUNCTION");

  fusorAddVariable("total", FUSOR_VARTYPE_INT);
  fusorAddVariable("left", FUSOR_VARTYPE_INT);
  fusorAddVariable("right", FUSOR_VARTYPE_INT);
  
  int left = analogRead(A0);
  int right = analogRead(A1);
  fusorSetIntVariable("left", left);
  fusorSetIntVariable("right", right);
  fusorSetIntVariable("total", left+right);
  avgSignal = left+right;
  
  FUSOR_LED_ON();
  delay(200);
  FUSOR_LED_OFF();
}

void loop() {
  fusorLoop();
  
  updateAll();
  delay(5);
}

void updateAll() {
  int left = analogRead(A0);
  int right = analogRead(A1);
  fusorSetIntVariable("left", left);
  fusorSetIntVariable("right", right);

  long newSignal = left+right;
  avgSignal = (avgSignal*(100-newPercent) + newSignal*newPercent)/100;

  fusorSetIntVariable("total", avgSignal);
}
