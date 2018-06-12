#include <Sleep_n0m1.h>
#include <RTClib.h>
#include <Wire.h>
#include <SPI.h>
#include <SD.h>

// SHT30 I2C address is 0x44(68)
#define Addr 0x45

RTC_DS1307 RTC;
Sleep sleep;
File myFile;
//Sd2Card card;

unsigned long sleepTime; //how long you want the arduino to sleep
const int chipSelect = 4;
uint16_t index = 0;

void setup()
{
  //Serial.print("\nInitializing SD card...");
  //  // we'll use the initialization code from the utility libraries
  //  // since we're just testing if the card is working!
  //  if (!card.init(SPI_HALF_SPEED, chipSelect)) {
  //    Serial.println("initialization failed. Things to check:");
  //    Serial.println("* is a card inserted?");
  //    Serial.println("* is your wiring correct?");
  //    Serial.println("* did you change the chipSelect pin to match your shield or module?");
  //    while (1);
  //  } else {
  //    Serial.println("Wiring is correct and a card is present.");
  //  }
  if (!SD.begin(4)) {
    Serial.println("initialization failed!");
    while (1);
  }
  //Serial.println("initialization done.");

  // Initialise I2C communication as MASTER
  Wire.begin();
  // Initialise serial communication, set baud rate = 9600
  Serial.begin(9600);
  RTC.begin();
  sleepTime = 55000; //set sleep time in ms, max sleep time is 49.7 days
  //  if (! RTC.isrunning()) {
  //    Serial.println("RTC is NOT running!");
  //    // following line sets the RTC to the date & time this sketch was compiled
  //    RTC.adjust(DateTime(__DATE__, __TIME__));
  //  }

}

void loop()
{

  delay(100); ////delays are just for serial print, without serial they can be removed
  //Serial.println("execute your code here");

  unsigned int data[6];
  index = index + 1;

  // Start I2C Transmission
  Wire.beginTransmission(Addr);
  // Send measurement command
  Wire.write(0x2C);
  Wire.write(0x06);
  // Stop I2C transmission
  Wire.endTransmission();
  delay(500);

  // Request 6 bytes of data
  Wire.requestFrom(Addr, 6);

  // Read 6 bytes of data
  // cTemp msb, cTemp lsb, cTemp crc, humidity msb, humidity lsb, humidity crc
  if (Wire.available() == 6)
  {
    data[0] = Wire.read();
    data[1] = Wire.read();
    data[2] = Wire.read();
    data[3] = Wire.read();
    data[4] = Wire.read();
    data[5] = Wire.read();
  }

  // Convert the data
  float cTemp = ((((data[0] * 256.0) + data[1]) * 175) / 65535.0) - 45;
  float fTemp = (cTemp * 1.8) + 32;
  float humidity = ((((data[3] * 256.0) + data[4]) * 100) / 65535.0);

//  // Output data to serial monitor
//  Serial.print("Relative Humidity : ");
//  Serial.print(humidity);
//  Serial.println(" %RH");
//  Serial.print("Temperature in Celsius : ");
//  Serial.print(cTemp);
//  Serial.println(" C");
//  Serial.print("Temperature in Fahrenheit : ");
//  Serial.print(fTemp);
//  Serial.println(" F");

  DateTime now = RTC.now();
  String s_year = (String)(now.year());
  String s_month = (String)(now.month());
  String s_day = (String)(now.day());
  String s_hour = (String)(now.hour());
  String s_minute = (String)(now.minute());
  String s_second = (String)(now.second());

  myFile = SD.open("logging.txt", FILE_WRITE);
  String message = (String)index + "," + (String)cTemp + "," + (String)humidity + "," +
                   (String)s_year + "-" + (String)s_month + "-" + (String)s_day + "," +
                   (String)s_hour + ":" + (String)s_minute + ":" + (String)s_second;
  if (myFile) {
    Serial.print("Writing to test.txt...");
    myFile.println(message);
    // close the file:
    myFile.close();
    Serial.println("done.");
  } else {
    // if the file didn't open, print an error:
    Serial.println("error opening logging.txt");
  }

  Serial.print("sleeping for ");
  Serial.println(sleepTime);
  delay(100); //delay to allow serial to fully print before sleep

  sleep.pwrDownMode(); //set sleep mode
  sleep.sleepDelay(sleepTime); //sleep for: sleepTime.
  //  Serial.print(now.year(), DEC);
  //  Serial.print('/');
  //  Serial.print(now.month(), DEC);
  //  Serial.print('/');
  //  Serial.print(now.day(), DEC);
  //  Serial.print(' ');
  //  Serial.print(now.hour() - 1, DEC);
  //  Serial.print(':');
  //  Serial.print(now.minute(), DEC);
  //  Serial.print(':');
  //  Serial.print(now.second(), DEC);
  //  Serial.println();
}
