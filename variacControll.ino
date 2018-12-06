const int PUL = 7; //define Pulse pin
const int DIR = 6; //define Direction pin
const int ENA = 5; //define Enable Pin
const int DELAY = 5000; 
const int STEPSPERREV = 400; //adjusted with SW1,SW2,SW3 set to ON OFF OFF
const double REVSPERVARIAC = 3;//2 + ((double) 11)/((double) 12); //adjusted with the limit set at 100V
const double MAXVOLTS = 100; //where the A0per is
int rotation = 0; //initialized with voltage at 0
void setup() {
  pinMode (PUL, OUTPUT);
  pinMode (DIR, OUTPUT);
  pinMode (ENA, OUTPUT);
  digitalWrite(DIR, LOW);
  digitalWrite(ENA, HIGH);
  overrideCurrentVolts(60);
  turnToVolts(10);
}

void zero(){ 
  //turns one and a half full variac revolutions towards 0, stopping when it hits the stopper. 
  //If the stopper fails, this will still zero the variac but will likely damage either the variac or the gears or both.
  digitalWrite(DIR, LOW);
  for(int i = 0; i < STEPSPERREV * REVSPERVARIAC * 1.5; i++){
      delayMicroseconds(DELAY);
      digitalWrite(PUL, LOW);
      digitalWrite(PUL, HIGH);
      if(analogRead(A0) + analogRead(A0) + analogRead(A0) + analogRead(A0) == 0){
        break;
      }
  }
  rotation = 0;
}

void overrideCurrentVolts(double volts){ 
  //Tells the system where it is right now, if you are initializing with the voltage not at 0
  rotation = voltsToSteps(volts);
}

double getCurrentVolts(){
  //tells you at what voltage the code thinks the variac is currently set
  return MAXVOLTS * ((((double) rotation)/STEPSPERREV)/REVSPERVARIAC);
}

int voltsToSteps(double volts){
  //convert volts to Steps
  return (volts/MAXVOLTS) * STEPSPERREV * REVSPERVARIAC;
}

void turnToVolts(double volts){
  //set the variac to a given voltage
  int steps = voltsToSteps(volts);
  turnToSteps(steps);
}

void turnToSteps(int steps){
  //turns the stepper motor to a given number of steps from 0
  if(rotation < steps){
    digitalWrite(DIR,HIGH);
    while(rotation < steps){
      rotation++;
      delayMicroseconds(DELAY);
      digitalWrite(PUL, LOW);
      digitalWrite(PUL, HIGH);
    }
  } else {
    digitalWrite(DIR,LOW);
    while(rotation > steps){
      rotation--;
      delayMicroseconds(DELAY);
      digitalWrite(PUL, LOW);
      digitalWrite(PUL, HIGH);
      if(analogRead(A0) + analogRead(A0) + analogRead(A0) + analogRead(A0) == 0){
        rotation = 0;
        return;
      }
    }
  }
}

void loop() {}

