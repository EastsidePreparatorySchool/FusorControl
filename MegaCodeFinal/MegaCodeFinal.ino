#include <Queue.h>

Queue<char[]> commands;

void setup() 
{
  Serial.begin(9600);
  
}

void loop() 
{
    //collects serial messages from the hardware buffer
  int num = Serial.available();
  int buffer[num];
  int i = 0;
  while (Serial.available() > 0) buffer[i++] = Serial.read();

  //code to process results in pairs
  //sometimes the full pair wont make it through in a single cycle
  if(!completed) 
  {
    code = buffer[0];
    process();
    completed = true;
  }
  
  for (int i = (completed) ? 0 : 1; i < num; i += 2) 
  {
    bp1 = buffer[i];
    if (i + 1 < num) 
    {
      code = buffer[i+1];
      handleBuffer();
    } 
    else 
    {
      completed = false;
      break;
    }
  }

}

void handleBuffer()
{  
  if (classifier == 99) //c
  {
    if (code == 48) send(ID); //c0
  } 
  else if (classifier == 112) //p
  {
    if (code == 48) turboOff(); //p0
    else if (code == 49) turboOn(); //p1
    else if (code == 50) turboFast(); //p2
    else if (code == 51) respond(); //p3
  } 
  else if (classifier == 114) //r
  {
    
  }
}
