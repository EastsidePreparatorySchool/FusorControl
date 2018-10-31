
/* 11 on pump -> 4 on Arduino
 * 7 on pump -> A0
 * 8 -> A1
 * 12 -> 3
 * 13 -> 2
 * 
 * 3 -> A3/17
 * 5 -> A2/16
 * 6 -> A4/18
*/
void setup() {
  pinMode(13, OUTPUT);
  pinMode(9,OUTPUT);
  Serial.begin(9600);
  pinMode(3, OUTPUT);
  digitalWrite(3,HIGH);
  pinMode(17,INPUT);
  pinMode(16,INPUT);
  pinMode(18,INPUT);
  pinMode(A0,INPUT);
  pinMode(A1,INPUT);
}

String ID = "PUMPCONTROLLER";
String delineater = "STOP";
boolean completed = true;
boolean slow = false;
int classifier;
int code;


boolean toggle = false;

void loop() {
  //collects serial messages from the hardware buffer
  int num = Serial.available();
  int buffer[num];
  int i = 0;
  while (Serial.available() > 0) buffer[i++] = Serial.read();

  //code to process results in pairs
  //sometimes the full pair wont make it through in a single cycle
  if(!completed) {
    code = buffer[0];
    process();
    completed = true;
  }
  
  for (int i = (completed) ? 0 : 1; i < num; i += 2) {
    classifier = buffer[i];
    if (i + 1 < num) {
      code = buffer[i+1];
      process();
    } else {
      completed = false;
      break;
    }
  }

  //send own information back
  //pump info, possibly radiation, etc
  //respond();
  delay(100);
}

void process() {
  send("message recieved: " +String(classifier) + " " + String(code));
  
  if (classifier == 99) {
    if (code == 48) send(ID); //c0
  } else if (classifier == 112) {
    if (code == 48) turboOff(); //p0
    else if (code == 49) turboOn(); //p1
    else if (code == 50) turboFast(); //p2
    else if (code == 51) respond(); //p3
  } else if (classifier == 114) {
    
  }
}

void turboOff() {
  toggle = false;
  digitalWrite(9,LOW);
  digitalWrite(3,HIGH);
}
void turboOn() {
  toggle = true;
  digitalWrite(9,HIGH);
  digitalWrite(3,LOW);
}
void turboFast() {
  toggle = true;
  digitalWrite(2,slow?HIGH:LOW);
  slow = !slow;
}



void respond() {
  send("toggle: " + String(toggle));
  send("psu:"+String(analogRead(A0)));
  send("speed:"+String(analogRead(A1)));
  send("error:"+String(digitalRead(17)) );
  /*digitalWrite(9,HIGH);
  delay(10);
  digitalWrite(9,LOW);*/
}


void send(String s) {
  Serial.print(s);
  Serial.print(delineater);
}
