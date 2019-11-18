//
// Fusor project - Diaphragm pressure sensor code for Arduino
// 
//

#include "fusor.h"


void setup(){
  fusorInit("DIAPHRAGM");
  
  // fixed analog input
  fusorAddVariable("diaphragm_adc", FUSOR_VARTYPE_INT);

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
  fusorSetIntVariable("diaphragm_adc", analogRead(A0));
}
  
  
 
