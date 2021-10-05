int sprey_pin = 12;
char wait_for_symbol = '1';

void press_sprey_button() {
  digitalWrite(sprey_pin, HIGH);
  delay(100);
  digitalWrite(sprey_pin, LOW);
}

void setup()
{
  pinMode(sprey_pin, OUTPUT);
  Serial.begin(9600);
}

void loop()
{
  if (Serial.available() > 0) {
    
    char bluetooth_value = Serial.read();
    if (bluetooth_value == wait_for_symbol) {
      press_sprey_button();
    }
    
  }
}
