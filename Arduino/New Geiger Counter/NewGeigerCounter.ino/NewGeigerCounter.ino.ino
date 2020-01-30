//
// Fusor project - Generic input arduino code for Arduino
// A0,A1,A2,A3 analog inputs
// D2,D3 digital input/outputs
// D2 digital count/frequency input
//

#include "fusor.h"

volatile static int d2_count;
volatile static long d2_timestamp;


void setup(){
  fusorInit("GENERIC");


  fusorAddVariable("d2_count", FUSOR_VARTYPE_INT);      // read only
  fusorAddVariable("d2_frequency", FUSOR_VARTYPE_FLOAT);// read only

  d2_timestamp = millis();
  d2_count = 0;

  attachInterrupt(digitalPinToInterrupt(2), ISR_edgeCounter, RISING);
  //this method of reading inputs provides us consistent values with an accuracy that the current fusor systems does not
  Serial.begin(9600);
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
  Serial.println(d2_count/2);
  updateAll();
  fusorDelay(1000);
}

void updateAll() {
  int count;
  long timestamp;
  long now;
  

 
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
 float frequency = ((float)count*1000)/period;

  fusorSetIntVariable("d2_count",count);
  fusorSetFloatVariable("d2_frequency",frequency);
}
  
  
 
