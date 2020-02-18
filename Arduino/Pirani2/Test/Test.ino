

void setup() {
  Serial.begin(115200);
  Serial.println("Hello");
  Serial3.begin(9600);
  pinMode(LED_BUILTIN, HIGH);
}

void loop() { 
  if (Serial.available()) {
    digitalWrite(LED_BUILTIN, HIGH);
    while (Serial.available()) {
      byte b = Serial.read();
      Serial3.write(b);
      Serial.write(b);
      delay(5);
    }
    Serial.println("");
    digitalWrite(LED_BUILTIN, LOW);
  }

  delay(100);

  if (Serial3.available()) {
    Serial.print("- ");
    while (Serial3.available()) {
      Serial.write(Serial3.read());
      delay(5);
    }
    Serial.println("");
  }
}
