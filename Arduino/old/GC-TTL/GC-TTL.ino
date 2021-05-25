//
// Fusor project - TTL Geiger counter (edge detect on D2)
// Arduino Uno
//

#include "fusor.h"

volatile static int d2_count;
volatile static long d2_timestamp;


void setup(){
  fusorInit("GC-TTL", 1000);
  
  // fixed analog input  
  fusorAddVariable("cps", FUSOR_VARTYPE_FLOAT);// read only

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
  fusorDelay(1000);
}

void updateAll() {
  int count;
  long timestamp;
  long now;
  
  noInterrupts();
  timestamp = d2_timestamp; // read the timestamp of last read-out
  d2_timestamp = millis();  // reset it
  now = d2_timestamp;       // also need current time stable for calculations
  count = d2_count;         // get the count since last read-out
  d2_count = 0;             // reset it
  interrupts();

  long period = now - timestamp;  // how long was that
  if (period != 0) // let's not crash
  {
    float cps = ((float)count)/period;
    fusorSetFloatVariable("cps",cps);
  }
}
  
  
 
