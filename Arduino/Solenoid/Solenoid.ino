//
// Fusor project code for control Arduino
// "SOLENOID" - solonoid valve relay controller
// Adafruit Feather Huzzah ESP8266 Wifi 
// Board support: http://arduino.esp8266.com/stable/package_esp8266com_index.json
//
//

#include "fusor.h"


#define SOL 2 // digital out pin for solenoid


FusorVariable fvs[] = {
  //name,           value,   updated
  {"solenoid",      "closed",false},
};


void setup(){
  // light for hope
  pinMode(LED_BUILTIN, OUTPUT);  // pin 13
  
  // relay control for solenoid valve
  pinMode(SOL, OUTPUT);
  digitalWrite(SOL, LOW);

  fusorInit("SOLENOID", fvs, 1);

  //zeroVoltage();
  FUSOR_LED_ON();
  delay(300);
  FUSOR_LED_OFF();
}



void loop() {
  // must do this in loop, the rest is optional
  fusorLoop();
  
  updateAll();
  delay(5);
}

void updateAll() {
  // if "solenoid" was updated, open or close it
  if (fusorVariableUpdated("solenoid")) {
    if (fusorStrVariableEquals("solenoid", "open")) {
      digitalWrite(SOL, HIGH);
    } else if(fusorStrVariableEquals("solenoid", "closed")){
      digitalWrite(SOL, LOW);
    }
  }
}
