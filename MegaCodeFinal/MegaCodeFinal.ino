#include "NewQueue.h"

Queue<String> commands;
String buffs = "";

#define MINVOLTS 5
#define MAXVOLTS 120 //incorrect

#define PUL 35
#define ENA 37
#define DIR 36

#define POT A2

#define delayMicros 1000


void setup() 
{
  pinMode(PUL, OUTPUT);
  pinMode(ENA, OUTPUT);
  pinMode(DIR, OUTPUT);

  pinMode(POT, INPUT);
  pinMode(13, OUTPUT);
  
  
  Serial.begin(9600);
  
  zeroVoltage();
}
String temp;
void loop() 
{
  //collects serial messages from the hardware buffer
  int num = Serial.available();
  char buffer[num];
  int i = 0;
  while (Serial.available() > 0) buffer[i++] = Serial.read();
  if(num>0) Serial.println("got " + String(num) + " bytesEND");
  buffs += String(buffer);
  int eindex = buffs.indexOf("END");
  temp = buffs.substring(0);
  temp.replace("END","end");
  if(temp.length()>0) Serial.println("buffer: " + temp+"END");
  //finds the END int the string, separates it, adds that command to the queue, 
  while(eindex != -1)
  {
    commands.enqueue(buffs.substring(0,eindex));
    buffs = buffs.substring(eindex+3);    
    eindex = buffs.indexOf("END");
  }

  //run every command in the buffer
  while(!commands.isEmpty())
  {
    handleBuffer(commands.dequeue());
  }

  delay(100);
}

String pre;
String cont;

void handleBuffer(String command)
{  
  
  //parses GET and SET commands and does things with them
  pre = command.substring(0,3);
  cont = command.substring(3);
  Serial.println(command);
  if(pre.equals("SET"))
  {
    if(cont.startsWith("volt")) 
    {
      int volts = cont.substring(4,7).toInt();
      setVoltage(volts);
      Serial.println("setvoltage" + String(volts) + "END");
    }

    if(cont.startsWith("tmp"))
    {
      if(cont.substring(3).equals("on"))
      {
        tmpOn();
        Serial.println("tmponEND");
      }
      
      if(cont.substring(3).equals("off"))
      {
        tmpOff();
        Serial.println("tmpoffEND");
      }
    }
  }

  if(pre.equals("GET"))
  {
    Serial.println("memeEND");
  }

  if(pre.equals("TES"))
  {
    
    Serial.println(cont+"END");
    digitalWrite(13,HIGH);
    delay(500);
    digitalWrite(13,LOW);
  }
}

void tmpOn()
{
  
}

void tmpOff()
{
  
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

  digitalWrite(ENA, HIGH);
  do {
    pot = analogRead(POT);
    dif = targetPot - pot;

    digitalWrite(DIR, (dif < 0) ? HIGH:LOW);
    
    digitalWrite(PUL, HIGH);
    delayMicroseconds(delayMicros);
    digitalWrite(PUL, LOW);
    delayMicroseconds(delayMicros);
    
  } while( abs(dif) > 10 );

  digitalWrite(ENA, LOW);
}
void zeroVoltage() {
  //set the variac as low as we can
  setVoltage(MINVOLTS);


  //drive down some bonus steps, so we get to actual zero
  digitalWrite(ENA, HIGH);
  digitalWrite(DIR, HIGH);
  for(int i = 0; i < 20; i++) {
    digitalWrite(PUL, HIGH);
    delayMicroseconds(delayMicros * 2);
    digitalWrite(PUL, LOW);
    delayMicroseconds(delayMicros * 2);
  }
  digitalWrite(ENA, LOW);
}
