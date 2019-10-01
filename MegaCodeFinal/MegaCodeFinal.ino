
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

#define LED_ON()  digitalWrite(LED_BUILTIN, HIGH);
#define LED_OFF() digitalWrite(LED_BUILTIN, LOW);


void setup()
{
  // light for hope
  pinMode(LED_BUILTIN, OUTPUT);  // pin 13

  // stepper control for varaic
  pinMode(PUL, OUTPUT);
  pinMode(ENA, OUTPUT);
  pinMode(DIR, OUTPUT);

  // stepper controller power relay
  pinMode(REL, OUTPUT);

  // feedback from variac
  pinMode(POT, INPUT);

  // turbo-molecular pump controller
  pinMode(TMP, OUTPUT);

  // solenoid gas injection valve control transistor
  pinMode(SOL, OUTPUT);


  // turn everything off
  tmpOff();
  //zeroVoltage();
  solOff();

  Serial.begin(9600);
  Serial.println("Fusor control Arduino initialized!");
}


void loop()
{
  static char buffer[CMDLENGTH + 1];


  //collects serial messages from the hardware buffer
  int num = 0;
  do {
    while (Serial.available() > 0)
    {
      buffer[num++] = Serial.read();
      buffer[num] = 0;
    }

    if (num > 0)
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
    if (contLength > 4 && strncmp(cont, "volt", 4) == 0) {
      int volts = atoi(cont + 4);
      setVoltage(volts);
      Serial.print("setvoltage");
      Serial.print(volts);
      Serial.println("END");
    }

    if (contLength > 2 && strncmp(cont, "tmp", 3) == 0) {
      if (strcmp(cont + 3, "on") == 0) {
        tmpOn();
        Serial.println("tmponEND");
      }

      if (strcmp(cont + 3, "off") == 0) {
        tmpOff();
        Serial.println("tmpoffEND");
      }
    }

    if (contLength > 2 && strncmp(cont, "sol", 3) == 0) {
      if (strcmp(cont + 3, "on") == 0) {
        solOn();
        Serial.println("solonEND");
      }

      if (strcmp(cont + 3, "off") == 0) {
        solOff();
        Serial.println("soloffEND");
      }
    }
  }

  if (strcmp(cmd, "GET") == 0) Serial.println("memeEND");

  if (strcmp(cmd, "TES") == 0) {
    Serial.print(cont);
    Serial.println("END");

    LED_ON();
    delay(500);
    LED_OFF();
  }
}

void tmpOn() {
  LED_ON();
  digitalWrite(TMP, LOW);
  delay(100);
  LED_OFF();
}

void tmpOff() {
  LED_ON();
  digitalWrite(TMP, HIGH);
  delay(100);
  LED_OFF();
}

void solOn() {
  LED_ON();
  digitalWrite(SOL, HIGH);
  delay(100);
  LED_OFF();
}
void solOff() {
  LED_ON();
  digitalWrite(SOL, LOW);
  delay(100);
  LED_OFF();
}




float potToVolts(int pot) {
  return map(pot, 0, 1024, MINVOLTS, MAXVOLTS);
}

int voltsToPot(int volts) {
  if (volts < MINVOLTS) return 0;
  return map(volts, MINVOLTS, MAXVOLTS, 0, 1024);
}

void setVoltage(int volts) {
  int pot;
  int targetPot = voltsToPot(volts);
  if (volts > 90) return;
  int dif;

  LED_ON();
  digitalWrite(ENA, LOW);
  digitalWrite(REL, HIGH);

  do {
    pot = analogRead(POT);
    dif = targetPot - pot;
    digitalWrite(DIR, (dif < 0) ? LOW : HIGH);

    digitalWrite(PUL, HIGH);
    delayMicroseconds(delayMicros);
    digitalWrite(PUL, LOW);
    delayMicroseconds(delayMicros);
  } while ( abs(dif) > 10 );

  digitalWrite(ENA, HIGH);
  digitalWrite(REL, LOW);
  LED_OFF();
}




void zeroVoltage() {
  //set the variac as low as we can
  setVoltage(MINVOLTS);

  //drive down some bonus steps, so we get to actual zero
  LED_ON();
  digitalWrite(ENA, LOW);
  digitalWrite(REL, HIGH);

  digitalWrite(DIR, LOW);
  for (int i = 0; i < 20; i++) {
    digitalWrite(PUL, HIGH);
    delayMicroseconds(delayMicros * 2);
    digitalWrite(PUL, LOW);
    delayMicroseconds(delayMicros * 2);
  }
  digitalWrite(ENA, HIGH);
  digitalWrite(REL, LOW);
  LED_OFF();
}
