//
// Fusor project - Generic input arduino code for Arduino
// A0,A1,A2,A3 analog inputs
// D2,D3 digital input/outputs
// D2 digital count/frequency input
//

#include "fusor.h"

volatile static int d2_count;
volatile static long d2_timestamp;
volatile static long d2_total;
//d2_count (counts per second) is the total number of ticks since the last Fusor update 
// d2_total is the total number of ticks recorded up until present time

void setup(){
  fusorInit("GC-2");
  // fixed analog input


  // fixed digital i/o
  fusorAddVariable("d2_count", FUSOR_VARTYPE_INT);      // read only
  fusorAddVariable("d2_frequency", FUSOR_VARTYPE_FLOAT);// read only
  fusorAddVariable("d2_total", FUSOR_VARTYPE_INT);// read only

  d2_timestamp = millis();
  d2_count = 0;
  d2_total = 0;
 // Serial.begin(9600);
  attachInterrupt(digitalPinToInterrupt(2), ISR_edgeCounter, RISING);

  FUSOR_LED_ON();
  delay(200);
  FUSOR_LED_OFF();
}

void ISR_edgeCounter() {
  ++d2_count;
}

void loop() {
  d2_total += (d2_count/2);

  // must do this in loop, the rest is optional
  fusorLoop();
  //Serial.println(d2_count/2);
  updateAll();
  
  fusorDelay(100);
}

void updateAll() {
  int count;
  long timestamp;
  long now;
  int total;
  
 
  //
  // process d2 count and frequency
  //
  
  noInterrupts();
  now = millis();
  timestamp = d2_timestamp;
  count = d2_count/2;
  // get tick
  d2_timestamp = now;
  total = d2_total;
  d2_count=0;
  interrupts();

  long period = now - timestamp;
  if (period == 0) period = 1;
  float frequency = ((float)count*100)/period;

  fusorSetIntVariable("d2_count",count);
  fusorSetFloatVariable("d2_frequency",frequency);
  fusorSetIntVariable("d2_total",total);
}
  
  
 
