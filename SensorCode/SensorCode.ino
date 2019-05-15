/*Inputs:
GC-1:  Geiger Counter – White one, sends digital pulses
  ????
PS: Pressure Sensor
  
XD: X-Ray Detector
  Single voltage through analog read

VM: Voltage things???
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


int vm1;
int vm2;
int xd;
int ps;
int msElapsed;
int readFreq = 1; //(in 100s of ms)
int sendFreq = 1; 

void setup() {
  // communicate with the computer 
  Serial.begin(9600);
}

void readAnalog() {
  vm1 = analogRead(VM1pin);
  vm2 = analogRead(VM2pin);
  xd = analogRead(XDpin);
  ps = analogRead(PSpin);
}

void sendMessage() {
  String message = "statusbegin";
  message += "VM1:" + String(vm1);
  message += "VM2:" + String(vm2);
  message += "XD:" + String(xd);
  message += "PS:" + String(ps);
  message += "statusend";
  Serial.println(message);
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
  delay(100);
}
