//
// Fusor project
// Pirani2
// Serial Reader for MSK 901 Pirani/Micro-piezo
// Arduino Mega 2560 (need extra UART, could use EPS32 as well)
//

#include "fusor.h"

void setup(){
  // must do this in init, the rest is optional
  fusorInit("PIRANI");
  fusorAddVariable("P1",FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("P2",FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("P3",FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("P4",FUSOR_VARTYPE_FLOAT);

  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(14, INPUT);
  pinMode(15, OUTPUT);
  
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
  byte beforeLast, last, current;
  int i = 0;
  last = 0;

  // read until end found
  do {
    while(Serial3.available()) {
      beforeLast = last;
      last = current;
      current = Serial3.read();
      responseBuffer[i++] = current;
    }
  } while(current != 'F' || last !='F' || beforeLast != ';');
  
  // terminate 
  responseBuffer[i-3] = 0;
  // check if success
  char *ack = strstr(responseBuffer, "ACK");
  if (ack == NULL) {
    return NULL;
  } 

  // return pointer to response content
  return ack + 3;
}


// send a command, wait for response, loop and blink if NAK
bool command(char *str) {
  Serial3.write(str);
  if (readResponse() == NULL) {
    while(true) {
      digitalWrite(LED_BUILTIN, HIGH);
      delay(1000);
      digitalWrite(LED_BUILTIN, LOW);
      delay(1000);
    }
  }
}

// query device and set all variables 
void updateAll() {
  queryPressure ("P1", 1);
  queryPressure ("P2", 2);
  queryPressure ("P3", 3);
  queryPressure ("P4", 4);
}
