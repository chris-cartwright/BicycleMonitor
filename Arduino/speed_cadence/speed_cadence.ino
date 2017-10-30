#include <Arduino.h>
#include <SPI.h>

#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"

// Send extra info to the BLE receiver
//#define EXTRA_INFO
//#define SERIAL_DEBUG

Adafruit_BluefruitLE_SPI ble(8, 7, 4);

unsigned long lastMillis = 0;
unsigned long loop_interval = 1000;
int packet_num = 0;
int num_reads = 0;

typedef struct {
  bool last_state;
  unsigned long last_read;
  long rotation_accumulated;
  short rotation_count;
  int pin;
} RPM;

RPM speed = { LOW, 0, 0, 0, 11 };
RPM cadence = { LOW, 0, 0, 0, 10 };

void update_rpm(RPM* values) {
  unsigned long now = millis();
  bool new_state = digitalRead(values->pin);
  // Detect rising edge
  if(values->last_state == LOW && new_state == HIGH) {
    values->rotation_accumulated += values->last_read - now;
    values->rotation_count++;
    values->last_read = now;
  }

  values->last_state = new_state;
}

void reset_rpm(RPM* values) {
  values->last_state = LOW;
  values->last_read = millis();
  values->rotation_accumulated = 0;
  values->rotation_count = 0;
}

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

  pinMode(speed.pin, INPUT);
  pinMode(cadence.pin, INPUT);

  // Turn on the pull-up resistors
  digitalWrite(speed.pin, HIGH);
  digitalWrite(cadence.pin, HIGH);

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
  update_rpm(&speed);
  update_rpm(&cadence);

  if (millis() - lastMillis < loop_interval) {
    return;
  }

  int speed_rpm = 60000.0 / ((float)speed.rotation_accumulated / (float)speed.rotation_count);
  int cadence_rpm = 60000.0 / ((float)cadence.rotation_accumulated / (float)cadence.rotation_count);

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
#endif

  ble.print("\n");

  num_reads = 0;

  lastMillis = millis();

  // Don't include the time it took to send data over Bluetooth
  reset_rpm(&speed);
  reset_rpm(&cadence);
}

