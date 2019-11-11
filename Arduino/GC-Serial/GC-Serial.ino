//
// Fusor project - Serial Geiger Counter
// Arduino Mega 2560 R3
//

#include "fusor.h"
static int cps = 0;

void setup(){
  // must do this in init, the rest is optional
  fusorInit("GC-SERIAL");
  fusorAddVariable("cps",FUSOR_VARTYPE_INT);
  fusorSetIntVariable("cps",0);

  cps = 0;

  pinMode(10, INPUT);
  pinMode(11, OUTPUT);
  
  Serial3.begin(9600);//, SERIAL_8N1);
    
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
  int current, last;
  if (Serial3.available()) {
    while(Serial3.available()) {
      last = current;
      current = Serial3.read();
    }
    FUSOR_LED_ON();
    delay(100);
    FUSOR_LED_OFF();
  fusorSetIntVariable("cps", (current * 256) + last);
  }
}
