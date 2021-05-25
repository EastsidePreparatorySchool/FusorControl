//
// Fusor project - SensorArray
// Arduino Mega 2560 R3
//
// Connects to 
// - a serial Geiger counter (Dr. Whitmer's Geiger counter on pin 14/15)
// - two edge-detected Geiger counters (ISRs on D2 and D3)
// - a PN-junction stolen from the SEM (A0 and A1 for the left and right half, only the total value is interesting)
// - a serial PIN-diode gamma sensor on Serial1 (pin 18/19)
//
//

#include "fusor.h"

volatile int d2 = 0; // d2 is inside enclosure
volatile int d3 = 0; // d3 is outside enclosure
static long timeLastPulseGc2 = 0;
static long timeLastPulseGc3 = 0;

void setup(){
  // must do this in init, the rest is optional
  fusorInit("SENSORARRAY");
  fusorAddVariable("gc1",FUSOR_VARTYPE_INT);
  fusorAddVariable("pin",FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("pnj",FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("gc2",FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("gc3",FUSOR_VARTYPE_FLOAT);
  fusorSetIntVariable("gc1",0);
  fusorSetFloatVariable("pin",0.0);
  fusorSetFloatVariable("pnj",0.0);
  fusorSetFloatVariable("gc1",0.0);
  fusorSetFloatVariable("gc2",0.0);

  Serial1.begin(9600); // PIN gamma sensor (8N1 ?)
  Serial3.begin(9600);  // Dr. Whitmer's Geiger counter (8N1)

  pinMode(2, INPUT);
  pinMode(3, INPUT);

  attachInterrupt(digitalPinToInterrupt(2), ISR2, RISING);
  attachInterrupt(digitalPinToInterrupt(3), ISR3, RISING);
  
  FUSOR_LED_ON();
  delay(200);
  FUSOR_LED_OFF();
}

void loop() {
  fusorLoop();
  
  updateAll();
  delay(5);
}

void updateAll() {
  static char text[12];
  static char *str = text;
  static float decayingAvgCps = 0;
  const float newFraction = 0.1;
 
  // read the latest message from the serial GC if there is one
  // format: low byte, high byte
  int current, last;
  if (Serial3.available()) {
    int bytes = Serial3.available();
    // drain the whole queue
    while(Serial3.available()) {
      last = current;
      current = Serial3.read();
    }
    // but only report if we read 2 bytes exactly
    if (bytes == 2) {
      fusorSetIntVariable("gc1", (current * 256) + last);
    }
  }

  // read the latest message from the PIN diode sensor if there is one
  if (Serial1.available()) {   
    while(Serial1.available()) {
      // format: <x02>M:1.24<CR><LF>
      // could assert "<0x02>M:" but won't
      char b = Serial1.read();
      if (b == 0x02) {
        // STX (start of transmission), reset buffer
        str = text;
        *str = 0;
      } else {
        if (b == 0x0D) {
          // CR, end of message
          *str = 0; // insert 0 instead of <CR>
          fusorSetFloatVariableFromString("pin", &text[2]); // skipping "M:"
        } else {
          *str++ = b;
        }
      }
    }
  }

  // read PNJ - Read a few times so the ADC can stabilize.
  
  int a0;
  for (int i = 0; i < 10; i++) 
  {
    a0 = analogRead(0);
  }
  fusorSetFloatVariable("pnj", a0*100/1023.0);


  //
  // get the edge-detected Geiger counts
  //
  long now;
  static long lastSec = 0; 
  int d2now, d3now;
  float interval;
  
  now = millis()/1000;
  if (now > lastSec)  // update only once per second
  {
    lastSec = now;
    noInterrupts();
    d2now = d2;
    d3now = d3;
    d2 = 0;
    d3 = 0;
    interrupts();
    
    fusorSetFloatVariable("gc2", d2now); // 2 edges per click (really, 2 pulses!)
    fusorSetFloatVariable("gc3", d3now); // same here
  }
}

void ISR2() 
{
  long now = micros();
  if (now > timeLastPulseGc2+2000){
    d2++;
    timeLastPulseGc2 = now;
  }
}

void ISR3()
{
  long now = micros();
  if (now > timeLastPulseGc3+2000){
    d3++;
    timeLastPulseGc3 = now;
  }
}
