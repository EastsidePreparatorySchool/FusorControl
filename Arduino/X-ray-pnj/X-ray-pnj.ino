//
// Fusor project - Generic template code for Arduino
//
// #define BLUETOOTH

#include "fusor.h"



FusorVariable fvs[] = {
  //name,   value,  updated
  {"left",   "",     false},
  {"right",  "",     false},
  {"total",    "",   false} 
};



#define delayMicros 1000


void setup(){
  // must do this in init, the rest is optional
  fusorInit("X-RAY-PNJ", fvs, 3);
  
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

// this is the actual Arduino-specific code
// needs to update variables so the fusor software can read them (like foo)
// needs to detect updates and act on them (like: this will blink <bar> times if bar is updated)

void updateAll() {
  // put our variables out there
  int left = analogRead(A0);
  int right = analogRead(A2);
  int total = left+right;
  fusorSetVariable("left", NULL, &left, NULL);
  fusorSetVariable("right", NULL, &right, NULL);
  fusorSetVariable("total", NULL, &total, NULL);
}
