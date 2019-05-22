#include "NewQueue.h"

Queue<String> commands;
String buffs = "";

#define PUL 35;
#define ENA 37;
#define DIR 36;


void setup() 
{
  Serial.begin(9600);
  pinMode(13, OUTPUT);
}

void loop() 
{
    //collects serial messages from the hardware buffer
  int num = Serial.available();
  char buffer[num];
  int i = 0;
  while (Serial.available() > 0) buffer[i++] = Serial.read();
  
  buffs = buffs + String(buffer);
  int eindex = buffs.indexOf("END");

  //finds the END int the string, separates it, adds that command to the queue, 
  while(eindex != -1)
  {
    commands.enqueue(buffs.substring(0,eindex));
    buffs = buffs.substring(eindex + 2);
    eindex = buffs.indexOf("END");
  }

  //run every command in the buffer
  while(!commands.isEmpty())
  {
    handleBuffer(commands.dequeue());
  }
    
  delay(100);
}

void handleBuffer(String command)
{  
  
  //parses GET and SET commands and does things with them
  String pre = command.substring(0,3);
  String cont = command.substring(3);
  
  if(pre.equals("SET"))
  {
    if(cont.startsWith("volt")) 
    {
      int volts = cont.substring(4,7).toInt();
      voltage(volts);
      Serial.println("setvoltage" + String(volts) + "END");
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

void voltage(int v)
{
  
}
