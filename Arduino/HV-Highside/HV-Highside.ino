//
// Fusor project code for HV-highside Arduino
// 
// Adafruit HUZZAH32-ESP32 Feather
// www.adafruit.com
// Board support URL http://arduino.esp8266.com/stable/package_esp8266com_index.json
//

#define CMDLENGTH  50

#define delayMicros 1000

#define LED_ON()  digitalWrite(LED_BUILTIN, HIGH);
#define LED_OFF() digitalWrite(LED_BUILTIN, LOW);


void setup(){
  // light for hope
  pinMode(LED_BUILTIN, OUTPUT);  // pin 13

  LED_ON();
  delay(1000);
  LED_OFF();

  Serial.begin(9600);
  //Serial.println("Fusor control Arduino initialized!");
}


void loop() {
  static char buffer[CMDLENGTH + 1];
  static char outbuffer[10];

  //collects serial messages from the hardware buffer
  int num = 0;
  do 
  {
    while (Serial.available() > 0)
    {
      buffer[num++] = Serial.read();
      buffer[num] = 0;
    }

    //    if (num > 0) {
    //      Serial.print("got ");
    //      Serial.print(itoa(num, outbuffer, 10));
    //      Serial.println(" bytesEND");
    //    }
  } while (strstr(buffer, "]END") == NULL);

  // got message, let's parse
  char *sCommand = buffer;
  char *sEnd = strstr(sCommand, "]END");

  while (sEnd != NULL)
  {
    // found at least one more command
    // terminate command string at "]END"
    *sEnd = 0;

    // echo command
    if (strlen(sCommand) > 0)
    {
      if (strncmp(sCommand, "FusorCommand[", 13) == 0)
      {
        Serial.print(sCommand);
        Serial.println("]END");
      
        // execute command
        handleBuffer(sCommand+13);
      }
    }
    
    // advance both pointers to next possible command
    sCommand = sEnd + 4;
    sEnd = strstr(sCommand, "]END");
  }
  
  delay(5);
}

void handleBuffer(char *command)
{
  // handle special case of identify first
  if (strcmp(command, "IDENTIFY") == 0) Serial.println("FusorResponse[IDENTIFY:HV-HIGHSIDE]END");
  
  //parses GET and SET commands and does things with them
  if (strncmp(command, "SET:",4) == 0) 
  {
    char *var = command + 4; // find variable name
    char *val = strstr(var, "=");  // look for "=" and value
    if (val != NULL) {
      *val++ = 0; // terminate variable name, advance value pointer past "="
    }
    setVariable(var,val);
  }

  if (strncmp(command, "GET:",4) == 0) 
  {
     char *var = command + 4; // find variable name
     getVariable(var);
  }
    
  LED_ON();
  delay(200);
  LED_OFF();
}

void setVariable(char *var, char *val) 
{
  // this is a generic example
  int num = atoi(val);
  Serial.print("FusorResponse[SET:");
  Serial.print(var);
  Serial.print("=");
  Serial.print(val);
  Serial.println("]END"); 
}

void getVariable(char *var) 
{
  // this is a generic example
  Serial.print("FusorResponse[GET:");
  Serial.print(var);
  Serial.print("=");
  Serial.print("IDKLOL");
  Serial.println("]END"); 
}

void testOn() {
  LED_ON();
  delay(100);
  LED_OFF();
}

void testOff() {
  LED_ON();
  delay(100);
  LED_OFF();
}
