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

  pinMode(14, INPUT);
  pinMode(15, OUTPUT);
  
  Serial3.begin(9600);//, SERIAL_8N1);
    
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
  int current, last;
  if (Serial3.available()) {
    while(Serial3.available()) {
      last = current;
      current = Serial3.read();
    }
    fusorSetIntVariable("cps", (current * 256) + last);
  }
}
