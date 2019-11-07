//
// Fusor project - Generic template code for Arduino
//
// #define BLUETOOTH

#include "fusor.h"



FusorVariable fvs[] = {
  //name,   value,  updated
  {"foo",   "",     false},
  {"bar",   "",     false} 
};



#define delayMicros 1000


void setup(){
  // light for hope
  pinMode(LED_BUILTIN, OUTPUT);  // pin 13

  // must do this in init, the rest is optional
  fusorInit("GENERIC", fvs, 2);
  
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
  static int count = 0;
  count++;

  // put our current command count into "foo"
  fusorSetVariable("foo", NULL, &count, NULL);

  // if "bar" was updated, read it and blink that many times
  if (fusorVariableUpdated("bar")) {
    int num = fusorGetIntVariable("bar");
    for (int i = 0; i < num; i++) {
      FUSOR_LED_ON();
      delay(100);
      FUSOR_LED_OFF();
      delay(100);
    }
  }
}
