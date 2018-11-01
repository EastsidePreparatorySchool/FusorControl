const int PUL = 7; //define Pulse pin
const int DIR = 6; //define Direction pin
const int ENA = 5; //define Enable Pin
const int DELAY = 5000; 
const int STEPSPERREV = 400; //adjusted with SW1,SW2,SW3 set to ON OFF OFF
const double REVSPERVARIAC = 3;//2 + ((double) 11)/((double) 12); //adjusted with the limit set at 100V
const double MAXVOLTS = 100; //where the stopper is
int rotation = 0; //initialized with voltage at 0
void setup() {
  pinMode (PUL, OUTPUT);
  pinMode (DIR, OUTPUT);
  pinMode (ENA, OUTPUT);
  digitalWrite(DIR, LOW);
  digitalWrite(ENA, HIGH);
}

void zero(){ 
  //turns two full variac revolutions counterclockwise, MAKE SURE YOU TRUST THE STOPPER
  digitalWrite(DIR, LOW);
  for(int i = 0; i < STEPSPERREV * REVSPERVARIAC * 1.5; i++){
      delayMicroseconds(DELAY);
      digitalWrite(PUL, LOW);
      digitalWrite(PUL, HIGH);
  }
  rotation = 0;
}

void setCurrentVolts(double volts){ 
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
    }
  }
}

void loop() {}

