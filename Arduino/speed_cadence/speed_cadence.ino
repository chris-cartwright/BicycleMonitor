#include <Arduino.h>
#include <SPI.h>

#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"

// Send extra info to the BLE receiver
//#define EXTRA_INFO

Adafruit_BluefruitLE_SPI ble(8, 7, 4);

int speed_counter = 0;
int cadence_counter = 0;
unsigned long lastMillis = 0;
unsigned long loop_interval = 2000;
int packet_num = 0;
bool last_speed_state = LOW;
bool last_cadence_state = LOW;
int num_reads = 0;

void error(const __FlashStringHelper*err) {
  Serial.println(err);
  bool state = HIGH;
  while (1) {
    digitalWrite(13, state);
    state = !state;
    delay(500);
  }
}

void waitConnection() {
  Serial.println("Waiting for connection...");
  digitalWrite(13, HIGH);
  while (!ble.isConnected()) {
    delay(500);
  }

  Serial.println(F("Connected."));
  digitalWrite(13, LOW);

  ble.setMode(BLUEFRUIT_MODE_DATA);
}

void setup(void) {
  pinMode(13, OUTPUT);

  // The following two lines seem to make the bluetooth connection
  // more stable on startup
  while (!Serial);
  delay(500);

  Serial.begin(115200);

  Serial.print(F("Initialising the Bluefruit LE module: "));

  if (!ble.begin(1)) {
    error(F("Couldn't find Bluefruit, make sure it's in command mode & check wiring?"));
  }

  Serial.println(F("OK!"));

  ble.echo(false);

  Serial.println("BLE info:");
  ble.info();

  // Turn down the noise
  ble.verbose(false);

  pinMode(9, INPUT);
  pinMode(10, INPUT);

  // Turn on the pull-up resistors
  digitalWrite(9, HIGH);
  digitalWrite(10, HIGH);

  waitConnection();

  Serial.println(F("Done setup."));
}

void loop(void) {
  if (!ble.isConnected()) {
    waitConnection();
    return;
  }

  if (Serial.available()) {
    switch (Serial.read()) {
      case 'T':
        loop_interval = Serial.parseInt();
        Serial.print("loop_interval: ");
        Serial.println(loop_interval);
        break;

      case 'R':
        ble.disconnect();
        waitConnection();
        return;
    }
  }

  num_reads++;
  bool new_state = digitalRead(9);
  // Detect rising edge
  if(last_speed_state == LOW && new_state == HIGH) {
    speed_counter++;
  }

  last_speed_state = new_state;

  new_state = digitalRead(10);
  // Detect rising edge
  if(last_cadence_state == LOW && new_state == HIGH) {
    cadence_counter++;
  }

  last_cadence_state = new_state;

  if (millis() - lastMillis < loop_interval) {
    return;
  }

  int speed_rpm = speed_counter / (loop_interval / 1000);
  int cadence_rpm = cadence_counter / (loop_interval / 1000);

  ble.print("S");
  ble.print(speed_rpm);

  ble.print("C");
  ble.print(cadence_rpm);

  // Used for debugging to make sure we're getting new packets
  packet_num++;
  ble.print("P");
  ble.print(packet_num);

#ifdef EXTRA_INFO
  ble.print("N");
  ble.print(num_reads);

  ble.print("RS");
  ble.print(speed_counter);

  ble.print("RC");
  ble.print(cadence_counter);
#endif

  ble.print("\n");

  speed_counter = 0;
  cadence_counter = 0;
  num_reads = 0;

  lastMillis = millis();
}

