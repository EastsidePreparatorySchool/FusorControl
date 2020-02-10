//
// Fusor project code for control Arduino
// "GAS" - gas controller
// Adafruit Feather Huzzah ESP8266 Wifi (wifi not used)
// Board support: http://arduino.esp8266.com/stable/package_esp8266com_index.json
//

#include "fusor.h"
#include <Servo.h>

#define SOL 15 // digital out pin for solenoid

Servo needlevalveservo;
int MIN_DEG = 0;
int MAX_DEG = 180;
int SERVO_PWM_PORT = 2;

int solstat = false;
int needlevalvestat = 0;

void setup(){
  fusorInit("GAS");

  fusorAddVariable("sol_in", FUSOR_VARTYPE_BOOL);
  fusorAddVariable("sol_stat", FUSOR_VARTYPE_BOOL);
  
  fusorAddVariable("nv_in", FUSOR_VARTYPE_INT);
  fusorAddVariable("nv_stat", FUSOR_VARTYPE_INT);
  fusorAddVariable("nv_angle", FUSOR_VARTYPE_INT);
  
  // relay control for solenoid valve
  pinMode(SOL, OUTPUT);
  digitalWrite(SOL, LOW);
  fusorSetBoolVariable("sol_stat", false);
  solstat = false;

  needleValve(0);
  needlevalveservo.attach(SERVO_PWM_PORT);
  fusorSetIntVariable("nv_stat", 0);
  fusorSetIntVariable("nv_angle", 0);
  needlevalvestat = 0;
  
  FUSOR_LED_ON();
  delay(300);
  FUSOR_LED_OFF();
}

void loop() {
  fusorLoop();
  updateAll();
  delay(5);
}

void updateAll() {
  if (fusorVariableUpdated("sol_in")) {
    solstat = fusorGetBoolVariable("sol_in");
    digitalWrite(SOL, solstat?HIGH:LOW);
  }
  if (fusorVariableUpdated("nv_in")) {
    needleValve(fusorGetIntVariable("nv_in"));
  }
  fusorSetBoolVariable("sol_stat", solstat);
  fusorSetIntVariable("nv_stat", needlevalvestat);
}

void needleValve(int percent) {
  int angle = MIN_DEG + (MAX_DEG - MIN_DEG) * percent / 100;
  fusorSetIntVariable("nv_angle", angle);
  needlevalvestat = percent;
  needlevalveservo.write(angle);
}
