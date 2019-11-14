//
// Fusor project - Diaphragm pressure sensor code for Arduino
// 
//

#include "fusor.h"


void setup(){
  fusorInit("PRESSURE-DIAPHRAGM");
  
  // fixed analog input
  fusorAddVariable("pressure", FUSOR_VARTYPE_INT);

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
  fusorSetIntVariable("pressure", analogRead(A0));
}
  
  
 
