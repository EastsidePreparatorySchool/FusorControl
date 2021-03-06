#include "NewQueue.h"
/*Inputs:
GC-1:  Geiger Counter – White one, sends digital pulses
  ????
PS: Pressure Sensor
  analog read voltage
  
XD: X-Ray Detector
  Single voltage through analog read

VM1&2: Voltage things???
  single voltage through analog read
  

Not Complete:
ND:  Neutron Detector
NDC:  Neutron Detector circuit
C:  Camera
BD: Bubble Detector
SG: Seismograph
FD: Fire Detector

*/

#define VM1pin A0
#define VM2pin A0
#define XDpin A0
#define PSpin A0
#define GCpin 2 // must be either 2 or 3 
const int PINtoRESET = -1; //should be wired to the reset pin

Queue<String> commands;
String buffs = "";

int vm1;
int vm2;
int xd;
int ps;
int msElapsed;
int readFreq = 1; //(in 100s of ms)
int sendFreq = 1;
int recieveFreq = 2;
int Gcount = 0;
int gc = 0;
long lastRead = millis();
long dt = 0;

void setup() {
  // communicate with the computer 
  Serial.begin(9600);
  attachInterrupt(digitalPinToInterrupt(GCpin),geigerInterrupt, RISING); //TODO: mode
  pinMode(PINtoRESET, INPUT);    // Just to be clear, as default is INPUT. Not really needed.
  digitalWrite(PINtoRESET, LOW);
}

void geigerInterrupt() {
   Gcount++;
}

void readAnalog() {
  vm1 = analogRead(VM1pin);
  vm2 = analogRead(VM2pin);
  xd = analogRead(XDpin);
  ps = analogRead(PSpin);
  long m = millis();
  gc = Gcount;
  Gcount = 0;
  dt = m - lastRead;
  lastRead = m;
}

void sendMessage() {
  String message = "statusbegin";
  message += "VM1:" + String(vm1);
  message += "VM2:" + String(vm2);
  message += "XD:" + String(xd);
  message += "PS:" + String(ps);
  message += "GC:" + String(gc);
  message += "DT:" + String(dt); // DT = delta time = time between last two reads
  message += "statusend";
  Serial.println(message);
}

void recieveInput() {
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
}

void handleBuffer(String command)
{  
  
  //parses GET and SET commands and does things with them
  String pre = command.substring(0,3);
  String cont = command.substring(3);
  
  if(pre.equals("KIL"))
  {
    pinMode(PINtoRESET, OUTPUT); 
    while(1); 
  }
}

void loop() {
  // put your main code here, to run repeatedly:
  msElapsed ++;
  if(msElapsed % readFreq == 0){
    readAnalog();
  }
  if(msElapsed % sendFreq == 0){
    sendMessage();
  }
  if(msElapsed % recieveFreq == 0){
    recieveInput();
  }
  delay(100);
}
