#include "fusor.h"

volatile static int d2_count;
volatile static long d2_timestamp;
const int AOUTpin=0;//the AOUT pin of the hydrogen sensor goes into analog pin A0 of the arduino

void setup() {
    fusorInit("GENERIC");
  
    // fixed analog input
    fusorAddVariable("a0", FUSOR_VARTYPE_INT);            // read/write

    // fixed digital i/o
    fusorAddVariable("d2", FUSOR_VARTYPE_BOOL);           // read/write, using d2 to stay away from d0 and d1 which are used for serial
    fusorAddVariable("d2_count", FUSOR_VARTYPE_INT);      // read only
    fusorAddVariable("d2_frequency", FUSOR_VARTYPE_FLOAT);// read only
    d2_timestamp = millis();
    d2_count = 0;

    pinMode(8, INPUT);
    pinMode(13, OUTPUT);

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
  int count;
  long timestamp;
  long now;
  
  // process a registers
  fusorSetIntVariable("a0", (analogRead(AOUTpin)*5)/1023));
 
  // process d registers
  fusorSetBoolVariable("d2", analogRead(8));

  //
  // process d2 count and frequency
  now = millis();
  timestamp = d2_timestamp;
  count = d2_count;
  d2_timestamp = now;
  //
  
  noInterrupts();
  d2_count = 0;
  interrupts();

  long period = now - timestamp;
  if (period == 0) period = 1;
  float frequency = ((float)count)/period;

  fusorSetIntVariable("d2_count",count);
  fusorSetFloatVariable("d2_frequency",frequency);
}
