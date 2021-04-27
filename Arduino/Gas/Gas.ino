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
int MIN_MS = 1000.0f;
int MAX_MS = 2000.0f;
int SERVO_PWM_PORT = 2;

int solstat = false;

void setup(){
  fusorInit("GAS");

  fusorAddVariable("sol_in", FUSOR_VARTYPE_BOOL);
  fusorAddVariable("sol_stat", FUSOR_VARTYPE_BOOL);
  
  fusorAddVariable("nv_in", FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("nv_angle", FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("nv_ms", FUSOR_VARTYPE_INT);
  
  // relay control for solenoid valve
  pinMode(SOL, OUTPUT);
  digitalWrite(SOL, LOW);
  fusorSetBoolVariable("sol_stat", false);
  solstat = false;

  needlevalveservo.attach(SERVO_PWM_PORT);
  needleValve(0.0f);
  
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
    needleValve(fusorGetFloatVariable("nv_in"));
  }
  fusorSetBoolVariable("sol_stat", solstat);
}

void needleValve(float percent) {
  int ms = (int) (MIN_MS + ((MAX_MS - MIN_MS) * percent) / 100.0f);
  fusorSetIntVariable("nv_ms", ms);
  needlevalveservo.writeMicroseconds(ms);
  
  float angle = ((float)(ms - MIN_MS))/(MAX_MS - MIN_MS) * 180.0f;
  fusorSetFloatVariable("nv_angle", angle);
}
