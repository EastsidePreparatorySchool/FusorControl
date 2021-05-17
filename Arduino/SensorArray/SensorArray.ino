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
volatile long lastCounterTime = 0;

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

  Serial2.begin(19200); // PIN gamma sensor (8N1 ?)
  Serial3.begin(9600);  // Dr. Whitmer's Geiger counter (8N1)

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
  // read the latest message from the serial GC if there is one
  int current, last;
  if (Serial3.available()) {
    while(Serial3.available()) {
      last = current;
      current = Serial3.read();
    }
    fusorSetIntVariable("gc1", (current * 256) + last);
  }

  // read the latest message from the PIN diode sensor if there is one
  if (Serial2.available()) {
    char text[24];
    char *str = text;
    while(Serial2.available()) {
      // format: ┐M:1.24<CR><LF>
      // could assert "┐M:" but won't
      char b = Serial2.read();
      if (b == 0x0D) 
      {
        b = 0; // insert 0 instead of <CR>
      }
      *str++ = b;
    }
    fusorSetFloatVariableFromString("pin", &text[3]); // skipping first 3
  }

  // read PNJ - both halves. Read both lines a few times so the ADC can stabilize.
  
  int a0, a1;
  for (int i = 0; i < 10; i++) 
  {
    a0 = analogRead(0);
  }
  for (int i = 0; i < 10; i++) 
  {
    a1 = analogRead(1);
  }
  fusorSetFloatVariable("pnj", (a0+a1)/2048.0);

  // get the edge-detected Geiger counts
  int now, lastTime, d2now, d3now;
  float interval;
  
  lastTime = lastCounterTime;
  if (millis() - last > 1000)  // update only once per second
  {
    noInterrupts();
    d2now = d2;
    d3now = d3;
    now = millis();
    d2 = 0;
    d3 = 0;
    lastCounterTime = now;
    interrupts();
    interval = (now-lastTime)/1000.0;
  
    fusorSetFloatVariable("gc2", (d2/2)/interval); // 2 edges per click (really, 2 pulses!)
    fusorSetFloatVariable("gc3", (d3/2)/interval); // same here
  }
}

void ISR2() 
{
  d2++;
}

void ISR3()
{
  d3++;
}
