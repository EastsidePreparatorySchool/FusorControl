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
static int tpm_position = 0;
static int tps_position = 0;
//d2_count (counts per second) is the total number of ticks since the last Fusor update 
// d2_total is the total number of ticks recorded up until present time
//array_position is the tpd (ticks per decisecond)array

static int tpdm[600];
static int tpds[10];
static int tpm;
static int tps;
//tpd is an array of ticks per decisecond over the last minute
//tpm is ticks per minute
//tps is ticks per second
void setup(){
  fusorInit("GC-2");

  int i;
  // fixed analog input
  for(i = 0; i < 600; i++){
    tpdm[i] = 0;
  }
  
  for(i = 0; i < 10; i++){
    tpds[i]=0;
  }
  

  // fixed digital i/o
  fusorAddVariable("tps", FUSOR_VARTYPE_INT);      // read only
  fusorAddVariable("d2_timestamp", FUSOR_VARTYPE_FLOAT);// read only
  fusorAddVariable("tpm", FUSOR_VARTYPE_INT);// read only

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
  // 
  fusorDelay(100);
}

void averageTicksPerMinute(){
  tpm = 0;
  tpdm[tpm_position] = d2_count;  
  int i;
  for(i = 0; i<600; i++){

      tpm += tpdm[i]
  }
  
  
  if(tpm_position==599){
    tpm_position = 0;  
  } else(
    tpm_position += 1;
    )
  
}

void averageTicksPerSecond{
  tps = 0;
  int i;
  for(i = 0; i < 10; i++){
    tps = tpds[i]
  }
  if(tps_position = 10){
    tps_position = 0;
  } else {
    tps_position++;
  }
}
void updateAll() {
  int count;
  long timestamp;
  long now;
  int total;
  
   averageTicksPerSecond();
   averageTicksPerMinute();
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

  /*long period = now - timestamp;
  if (period == 0) period = 1;
  float frequency = ((float)count*100)/period;
  */
  fusorSetIntVariable("tps",tps);
  fusorSetFloatVariable("d2_timestamp",d2_timestamp);
  fusorSetIntVariable("tpm",tpm);
}
  
  
 
