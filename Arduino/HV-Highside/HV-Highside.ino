//
// Fusor project code for HV-highside Arduino
// 
// Adafruit HUZZAH32-ESP32 Feather
// www.adafruit.com
// Board support URL https://dl.espressif.com/dl/package_esp32_index.json
// Bluetooth tutprial: https://randomnerdtutorials.com/esp32-bluetooth-classic-arduino-ide/
//

#define CMDLENGTH  50

#define delayMicros 1000

#define LED_ON()  digitalWrite(LED_BUILTIN, HIGH);
#define LED_OFF() digitalWrite(LED_BUILTIN, LOW);


#include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

BluetoothSerial SerialBT;


void setup(){
  // light for hope
  pinMode(LED_BUILTIN, OUTPUT);  // pin 13

  LED_ON();
  delay(1000);
  LED_OFF();

  Serial.begin(9600);
  //Serial.println("Fusor control Arduino initialized!");
  SerialBT.begin("FUSOR_HV_HIGHSIDE"); //Bluetooth device name
  }


void loop() {
  static char buffer[CMDLENGTH + 1];
  static char outbuffer[10];

  //collects serial messages from the hardware buffer
  int num = 0;
  do 
  {
    while (SerialBT.available() > 0)
    {
      buffer[num++] = SerialBT.read();
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
        SerialBT.print(sCommand);
        SerialBT.println("]END");
      
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
  if (strcmp(command, "IDENTIFY") == 0) SerialBT.println("FusorResponse[IDENTIFY:HV-HIGHSIDE]END");
  
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
  SerialBT.print("FusorResponse[SET:");
  SerialBT.print(var);
  SerialBT.print("=");
  SerialBT.print(val);
  SerialBT.println("]END"); 
}

void getVariable(char *var) 
{
  // this is a generic example
  SerialBT.print("FusorResponse[GET:");
  SerialBT.print(var);
  SerialBT.print("=");
  SerialBT.print("IDKLOL");
  SerialBT.println("]END"); 
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
