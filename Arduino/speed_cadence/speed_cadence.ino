#include <Arduino.h>
#include <SPI.h>

#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"

//#define TIME_LOOP

Adafruit_BluefruitLE_SPI ble(8, 7, 4);

unsigned int speed_counter;
unsigned int cadence_counter;
unsigned long lastMillis = 0;
unsigned long loop_interval = 2000;

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

  attachInterrupt(0, speed_interrupt, FALLING);
  attachInterrupt(1, cadence_interrupt, FALLING);
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

  waitConnection();

  Serial.println(F("Done setup."));
}

void loop(void) {
  if (!ble.isConnected()) {
    detachInterrupt(0);
    detachInterrupt(1);
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
        detachInterrupt(0);
        detachInterrupt(1);
        ble.disconnect();
        waitConnection();
        return;
    }
  }

  if (millis() - lastMillis < loop_interval) {
    return;
  }

  detachInterrupt(0);
  detachInterrupt(1);

  int speed_rpm = speed_counter / (loop_interval / 1000);
  int cadence_rpm = cadence_counter / (loop_interval / 1000);

  ble.print("S");
  ble.print(speed_rpm);

  ble.print("C");
  ble.print(cadence_rpm);

  ble.print("\n");

  speed_counter = 0;
  cadence_counter = 0;

  lastMillis = millis();

  attachInterrupt(0, speed_interrupt, FALLING);
  attachInterrupt(1, cadence_interrupt, FALLING);
}

void speed_interrupt() {
  speed_counter++;
}

void cadence_interrupt() {
  cadence_counter++;
}

