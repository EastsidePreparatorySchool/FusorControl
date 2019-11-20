//
// Fusor project code for control Arduino
// "NEEDLEVALVE" - gas controller
// Arduino Uno
//

#include "fusor.h"

void setup(){
  fusorInit("NEEDLEVALVE");
  fusorAddVariable("needlevalve", FUSOR_VARTYPE_INT);
  fusorSetIntVariable("needlevalve", 0);

  needleValve(0);

  FUSOR_LED_ON();
  delay(300);
  FUSOR_LED_OFF();
}

void loop() {
  fusorLoop();
  updateAll();
  delay(5);
}

void updateAll() {
  if (fusorVariableUpdated("needlevalve")) {
    needleValve(fusorGetIntVariable("needlevalve"));
  }
}

void needleValve(int percent) {
  // TODO: Write code for needle valve controller here
}
