//
// Fusor project code for control Arduino
// "GAS" - gas controller
// Adafruit Feather Huzzah ESP8266 Wifi (wifi not used)
// Board support: http://arduino.esp8266.com/stable/package_esp8266com_index.json
//

#include "fusor.h"

#define SOL 15 // digital out pin for solenoid
Servo needlevalveservo;
int MIN_DEG = 0;
int MAX_DEG = 180;
int SERVO_PWM_PORT = 2;

void setup(){
  fusorInit("GAS");
  fusorAddVariable("solenoid", FUSOR_VARTYPE_BOOL);
  fusorAddVariable("needlevalve", FUSOR_VARTYPE_INT);
  fusorSetBoolVariable("solenoid", false);
  fusorSetIntVariable("needlevalve", 0);
  fusorSetIntVariable("nv_angle", 0);


  // relay control for solenoid valve
  pinMode(SOL, OUTPUT);
  digitalWrite(SOL, LOW);

  needleValve(0);
  needlevalveservo.attach(SERVO_PWM_PORT);
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
  if (fusorVariableUpdated("solenoid")) {
    digitalWrite(SOL, fusorGetBoolVariable("solenoid")?HIGH:LOW);
  }
  if (fusorVariableUpdated("needlevalve")) {
    needleValve(fusorGetIntVariable("needlevalve"));
  }
}

void needleValve(int percent) {
  int angle = MIN_DEG + (MAX_DEG - MIN_DEG) * percent / 100;
  fusorSetIntVariable("nv_angle", angle);

  needlevalveservo.write(angle);

  // the servo library doesn't like being talked to while the servo is still settling
  fusorDelay(1200);
  fusorClearCommandQueue();
}
