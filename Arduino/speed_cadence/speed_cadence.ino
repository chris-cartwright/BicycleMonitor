#include <Arduino.h>
#include <SPI.h>

#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"

// Send extra info to the BLE receiver
//#define EXTRA_INFO
//#define SERIAL_DEBUG

Adafruit_BluefruitLE_SPI ble(8, 7, 4);

int speed_counter = 0;
int cadence_counter = 0;
unsigned long lastMillis = 0;
unsigned long loop_interval = 2000;
int packet_num = 0;
bool last_speed_state = LOW;
bool last_cadence_state = LOW;
int num_reads = 0;

const int speed_pin = 11;
const int cadence_pin = 10;

void serial_print(const char* msg) {
#if SERIAL_DEBUG
  Serial.print(msg);
#endif
}

void serial_println(const char* msg) {
#if SERIAL_DEBUG
  Serial.println(msg);
#endif
}

void serial_print(const __FlashStringHelper* msg) {
#if SERIAL_DEBUG
  Serial.print(msg);
#endif
}

void serial_println(const __FlashStringHelper* msg) {
#if SERIAL_DEBUG
  Serial.println(msg);
#endif
}

void error(const __FlashStringHelper*err) {
  serial_println(err);
  
  bool state = HIGH;
  while (1) {
    digitalWrite(13, state);
    state = !state;
    delay(500);
  }
}

void waitConnection() {
  serial_println("Waiting for connection...");

  digitalWrite(13, HIGH);
  while (!ble.isConnected()) {
    delay(500);
  }

  serial_println(F("Connected."));
  digitalWrite(13, LOW);

  ble.setMode(BLUEFRUIT_MODE_DATA);
}

void setup(void) {
  pinMode(13, OUTPUT);
  digitalWrite(13, HIGH);

#if SERIAL_DEBUG
  // The following two lines seem to make the bluetooth connection
  // more stable on startup
  while (!Serial);
  delay(500);
  
  Serial.begin(115200);
#endif

  serial_print(F("Initialising the Bluefruit LE module: "));

  if (!ble.begin(1)) {
    error(F("Couldn't find Bluefruit, make sure it's in command mode & check wiring?"));
  }

  serial_println(F("OK!"));

#if SERIAL_DEBUG
  ble.echo(false);
#endif

  //Serial.println("BLE info:");
  //ble.info();

  // Turn down the noise
  ble.verbose(false);

  pinMode(speed_pin, INPUT);
  pinMode(cadence_pin, INPUT);

  // Turn on the pull-up resistors
  digitalWrite(speed_pin, HIGH);
  digitalWrite(cadence_pin, HIGH);

  serial_println(F("Done setup."));

  int blinker = LOW;
  digitalWrite(13, blinker);
  delay(1000);
  for(int i = 0; i < 10; i++) {
    blinker = !blinker;
    digitalWrite(13, blinker);
    delay(500);
  }

  waitConnection();
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
  bool new_state = digitalRead(speed_pin);
  // Detect rising edge
  if(last_speed_state == LOW && new_state == HIGH) {
    speed_counter++;
  }

  last_speed_state = new_state;

  new_state = digitalRead(cadence_pin);
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

