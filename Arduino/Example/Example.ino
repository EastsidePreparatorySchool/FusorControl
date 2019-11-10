//
// Fusor project - Example code for Arduino
//

#include "fusor.h"



void setup(){
  // must do this in init, the rest is optional
  fusorInit("EXAMPLE");
  fusorAddVariable("random", FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("blink", FUSOR_VARTYPE_INT);
  fusorAddVariable("fifty-fifty", FUSOR_VARTYPE_BOOL);
  fusorAddVariable("marco", FUSOR_VARTYPE_STR);
  
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
  // put a random float into "random"
  float r = random(1000000)/1000000.0f;
  fusorSetFloatVariable("random", r);
  
  // put a true or false into "fifty-fifty" based on "random"
  fusorSetBoolVariable("fifty-fifty", r>=0.5);
  
  // put a string response into "marco"
  fusorSetStrVariable("marco","polo");
  
  // if "blink" was updated, read it and blink that many times
  if (fusorVariableUpdated("blink")) {
    int num = fusorGetIntVariable("blink");
    for (int i = 0; i < num; i++) {
      FUSOR_LED_ON();
      delay(100);
      FUSOR_LED_OFF();
      delay(100);
    }
  }
  }
