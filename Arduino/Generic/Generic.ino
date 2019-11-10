//
// Fusor project - Generic template code for Arduino
//

#include "fusor.h"

volatile static int d2_count;
volatile static long d2_timestamp;


void setup(){
  // must do this in init, the rest is optional
  fusorInit("GENERIC");
  
  // fixed analog input
  fusorAddVariable("a0", FUSOR_VARTYPE_INT);            // read/write
  fusorAddVariable("a1", FUSOR_VARTYPE_INT);            // read/write
  fusorAddVariable("a3", FUSOR_VARTYPE_INT);            // read/write 
  fusorAddVariable("a4", FUSOR_VARTYPE_INT);            // read/write 


  // fixed digital i/o
  fusorAddVariable("d2", FUSOR_VARTYPE_BOOL);           // read/write, using d2 to stay away from d0 and d1 which are used for serial
  fusorAddVariable("d3", FUSOR_VARTYPE_BOOL);           // read/write, using d3 to stay away from d0 and d1 which are used for serial
  fusorAddVariable("d2_count", FUSOR_VARTYPE_INT);      // read only
  fusorAddVariable("d2_frequency", FUSOR_VARTYPE_FLOAT);// read only

  d2_timestamp = millis();
  d2_count = 0;

  attachInterrupt(digitalPinToInterrupt(2), ISR_edgeCounter, RISING);

  FUSOR_LED_ON();
  delay(200);
  FUSOR_LED_OFF();
}

void ISR_edgeCounter() {
  ++d2_count;
}

void loop() {
  // must do this in loop, the rest is optional
  fusorLoop();
  
  updateAll();
  delay(5);
}

void updateAll() {
  int val;
  char *aRegs[] = {"a0","a1","a2","a3"};
  char *dRegs[] = {"d2","d3"};
  int count;
  long timestamp;
  long now;
  

  //
  // process a registers
  //
  for (int i = 0; i < 4; i++) {
    if (fusorVariableUpdated(aRegs[i])) {
      int val = fusorGetIntVariable(aRegs[i]);
      analogWrite(i, val);
    }
    fusorSetIntVariable(aRegs[i], analogRead(i));
  }
 
  //
  // process d registers
  //
  for (int i = 0; i < 2; i++) {
    if (fusorVariableUpdated(dRegs[i])) {
      bool val = fusorGetBoolVariable(dRegs[i]);
      digitalWrite(i, val?HIGH:LOW);
    }
    fusorSetIntVariable(dRegs[i], digitalRead(i) == HIGH);
  }

  //
  // process d2 count and frequency
  //
  
  noInterrupts();
  now = millis();
  timestamp = d2_timestamp;
  count = d2_count;
  d2_timestamp = now;
  d2_count = 0;
  interrupts();

  long period = now - timestamp;
  if (period == 0) period = 1;
  float frequency = ((float)count)/period;

  fusorSetIntVariable("d2_count",count);
  fusorSetFloatVariable("d2_frequency",frequency);
}
  
  
 
