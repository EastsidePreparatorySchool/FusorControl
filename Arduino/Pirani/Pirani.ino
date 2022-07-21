//
// Fusor project
// PIRANI
// Serial Reader for MSK 901 Pirani/Micro-piezo
// Arduino AVR boards/Arduino Mega 2560 
// (we need the extra UART, could use ESP32 as well)
//

#include "fusor.h"


long avgSignal;
int newPercent = 10;


void setup(){
  // must do this in init, the rest is optional
  fusorInit("PIRANI");
  //fusorAddVariable("p1",FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("p2",FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("p3",FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("p4",FUSOR_VARTYPE_FLOAT);
  //fusorAddVariable("pa_adc",FUSOR_VARTYPE_INT);
  //fusorAddVariable("pa",FUSOR_VARTYPE_FLOAT);

  //avgSignal = analogRead(A0);

  pinMode(LED_BUILTIN, OUTPUT);

  delay(3000);
  
  Serial3.begin(9600);//, SERIAL_8N1);

  command("@254GT!AIR;FF");       // use air calibration curves
  command("@254U!TORR;FF");       // report pressure in Torr
    
  command("@254TST!ON;FF");       // flash transducer LED
  FUSOR_LED_ON();
  delay(200);
  FUSOR_LED_OFF();
  command("@254TST!OFF;FF");      // flash transducer LED
}

void loop() {
  fusorLoop();
  
  updateAll();
  delay(100);
}

void queryPressure(const char * var, int mode) {
  Serial3.write("@254PR");          // To any device, query pressure
  Serial3.write((byte)('0'+mode));  // number <mode> (1=m-pirani, 2=m-piezo, 3=combo 3dig, 4=combo 4dig
  Serial3.write("?;FF");            // ok done, please respond now
  
  char *str = readResponse();
  if (str != NULL) {
    fusorSetFloatVariableFromString(var, str);
  }
}


// read until response ends in ";FF"
char * readResponse() {
  static char responseBuffer[30];
  char * pTerm;
  char *pAck;
  int i = 0;
  int count = 0;

  // read until end found
  do {
    digitalWrite(LED_BUILTIN, HIGH);
    while(Serial3.available()) {
      responseBuffer[i++] = Serial3.read();
    }
    digitalWrite(LED_BUILTIN, LOW);
    count++;
    fusorDelay(5);
  } while(count < 20 && !(pTerm = strstr(responseBuffer, ";FF")));
  
  // terminate 
  if (pTerm == NULL) {
    return NULL;
  } 
  *pTerm = 0;
  
  // check if success
  pAck = strstr(responseBuffer, "ACK");
  if (pAck == NULL) {
    return NULL;
  } 

  // return pointer to response content
  return pAck + 3;
}


// send a command, wait for response, loop and blink if NAK
bool command(char *str) {
  Serial3.write(str);
  fusorDelay(50);
  if (readResponse() == NULL) {
    while(true) {
      digitalWrite(LED_BUILTIN, HIGH);
      fusorDelay(500);
      digitalWrite(LED_BUILTIN, LOW);
      fusorDelay(500);
    }
  }
}

// query device and set all variables 
void updateAll() {
  //queryPressure ("p1", 1);
  queryPressure ("p2", 2);
  queryPressure ("p3", 3);
  queryPressure ("p4", 4);
//  long newSignal = analogRead(A0);
//  avgSignal = (avgSignal*(100-newPercent) + newSignal*newPercent)/100;
  //fusorSetIntVariable("pa_adc", avgSignal);
  //fusorSetFloatVariable("pa", 0); // todo: real math for voltage divider and log scale here
}
