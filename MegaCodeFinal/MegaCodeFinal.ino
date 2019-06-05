
//
// Fusor project code for control Arduino
//

#define CMDLENGTH  50

#define MINVOLTS 5
#define MAXVOLTS 120

#define PUL 35
#define ENA 37
#define DIR 36

#define TMP 43
#define SOL 10

#define REL 9

#define POT A2

#define delayMicros 1000


void setup() 
{
  pinMode(PUL, OUTPUT);
  pinMode(ENA, OUTPUT);
  pinMode(DIR, OUTPUT);

  pinMode(POT, INPUT);
  pinMode(13, OUTPUT);

  pinMode(REL, OUTPUT);
  
  
  Serial.begin(9600);
  pinMode(TMP, OUTPUT);
  pinMode(SOL, OUTPUT);
  digitalWrite(TMP, HIGH);
  zeroVoltage();
  Serial.println("done!");
}


void loop() 
{
  static char buffer[CMDLENGTH+1];

  
  //collects serial messages from the hardware buffer
  int num = 0;
  do {
    while (Serial.available() > 0) 
    {
      buffer[num++] = Serial.read();
      buffer[num] = 0;    
    }
    
    if(num>0) 
    {
      Serial.println("got " + String(num) + " bytesEND");
    }
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
    if(strlen(sCommand)>0) 
    {
      Serial.print("command: ");
      Serial.print(sCommand);
      Serial.println("END");
    }

    // execute command
    handleBuffer(sCommand);

    // advance both pointer to next possible command
    sCommand = sEnd+3;
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

  if(strcmp(cmd, "SET") == 0) {
    if(contLength > 4 && strncmp(cont, "volt", 4) == 0) {
      int volts = atoi(cont+4);
      setVoltage(volts);
      Serial.print("setvoltage");
      Serial.print(volts);
      Serial.println("END");
    }

    if(contLength > 2 && strncmp(cont, "tmp", 3) == 0) {
      if(strcmp(cont+3, "on") == 0) {
        tmpOn();
        Serial.println("tmponEND");
      }
      
      if(strcmp(cont+3, "off") == 0) {
        tmpOff();
        Serial.println("tmpoffEND");
      }
    }

    if(contLength > 2 && strncmp(cont, "sol", 3) == 0) {
      if(strcmp(cont+3, "on") == 0) {
        solOn();
        Serial.println("solonEND");
      }
      
      if(strcmp(cont+3, "off") == 0) {
        solOff();
        Serial.println("soloffEND");
      }
    }
  }

 if(strcmp(cmd, "GET") == 0) Serial.println("memeEND");

 if(strcmp(cmd, "TES") == 0) {
    Serial.print(cont);
    Serial.println("END");
    
    digitalWrite(13,HIGH);
    delay(500);
    digitalWrite(13,LOW);
  }
}

void tmpOn() {
  digitalWrite(TMP, LOW);
}

void tmpOff() {
  digitalWrite(TMP, HIGH);
}

void solOn() {
  digitalWrite(SOL, HIGH);
}
void solOff() {
  digitalWrite(SOL, LOW);
}







float potToVolts(int pot) { return map(pot, 0, 1024, MINVOLTS, MAXVOLTS); }
int voltsToPot(int volts) {
  if (volts < MINVOLTS) return 0;
  return map(volts, MINVOLTS,MAXVOLTS, 0, 1024);
}

void setVoltage(int volts) {
  int pot;
  int targetPot = voltsToPot(volts);
  if(volts>90) return;
  int dif;

  digitalWrite(ENA, LOW);
  digitalWrite(REL, HIGH);
  
  do {
    pot = analogRead(POT);
    dif = targetPot - pot;
    digitalWrite(DIR, (dif < 0) ? LOW:HIGH);
    
    digitalWrite(PUL, HIGH);
    delayMicroseconds(delayMicros);
    digitalWrite(PUL, LOW);
    delayMicroseconds(delayMicros);
    
  } while( abs(dif) > 10 );

  digitalWrite(ENA, HIGH);
  digitalWrite(REL, LOW);
}
void zeroVoltage() {
  //set the variac as low as we can
  setVoltage(MINVOLTS);


  //drive down some bonus steps, so we get to actual zero
  digitalWrite(ENA, LOW);
  digitalWrite(REL, HIGH);
  
  digitalWrite(DIR, LOW);
  for(int i = 0; i < 20; i++) {
    digitalWrite(PUL, HIGH);
    delayMicroseconds(delayMicros * 2);
    digitalWrite(PUL, LOW);
    delayMicroseconds(delayMicros * 2);
  }
  digitalWrite(ENA, HIGH);
  digitalWrite(REL, LOW);
}
