
//
// Fusor project code for control Arduino
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
  } while (strstr(buffer, "END") == NULL);

  // got message, let's parse
  char *sCommand = buffer;
  char *sEnd = strstr(sCommand, "END");

  while (sEnd != NULL)
  {
    // found at least one more command
    // terminate command string at "END"
    *sEnd = 0;

    // echo command
    if (strlen(sCommand) > 0)
    {
      Serial.print("command: ");
      Serial.print(sCommand);
      Serial.println("END");
    }

    // execute command
    handleBuffer(sCommand);

    // advance both pointer to next possible command
    sCommand = sEnd + 3;
    sEnd = strstr(sCommand, "END");
  }
  
  delay(5);
}

void handleBuffer(char *command)
{
  char *cont;
  int cmdLength = strlen(command);

  // check here for malformed command
  if (cmdLength < 3) return;

  char cmd[4];
  strncpy (cmd, command, 3);
  cmd[3] = 0;

  //parses GET and SET commands and does things with them
  cont = command + 3;
  int contLength = strlen(cont);

  if (strcmp(cmd, "SET") == 0) {
    if (contLength > 4 && strncmp(cont, "test", 4) == 0) {
      int num = atoi(cont + 4);
      // do something with num
      Serial.print("set test");
      Serial.print(num);
      Serial.println("END");
    }
  }
    
  if (strcmp(cmd, "GET") == 0) Serial.println("memeEND");
  if (strcmp(cmd, "IDE") == 0) Serial.println("IDEGENERICTESTEND");
  if (strcmp(cmd, "TES") == 0) {
    Serial.print(cont);
    Serial.println("END");

    LED_ON();
    delay(500);
    LED_OFF();
  }
  
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
