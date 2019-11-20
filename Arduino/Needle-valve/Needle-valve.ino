//
// Fusor project code for control Arduino
// "NEEDLEVALVE" - gas controller
// Arduino Uno
//

#include "fusor.h"
#include <Servo.h>

Servo needlevalveservo;
int MIN_DEG = 0;
int MAX_DEG = 180;
int SERVO_PWM_PORT = 9;

void setup(){
  fusorInit("NEEDLEVALVE");
  fusorAddVariable("needlevalve", FUSOR_VARTYPE_INT);
  fusorAddVariable("nv_angle", FUSOR_VARTYPE_INT);
  fusorSetIntVariable("needlevalve", 0);
  fusorSetIntVariable("nv_angle", 0);

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
  if (fusorVariableUpdated("needlevalve")) {
    needleValve(fusorGetIntVariable("needlevalve"));
  }
}

void needleValve(int percent) {
  int angle = MIN_DEG + (MAX_DEG - MIN_DEG) * percent / 100;
  fusorSetIntVariable("nv_angle", angle);

  needlevalveservo.write(angle);
}
