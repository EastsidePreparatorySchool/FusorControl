#include <SPI.h>
#include <SD.h>

const int chipSelect = 4;

void setup() {
  // see if the card is present and can be initialized:
  if (!SD.begin(chipSelect)) {
    // don't do anything more:
    while (1);
  }
  if(SD.exists("data.txt")){
    SD.remove("data.txt");
  }
  delay(100);
  if(SD.exists("data.txt")){
    SD.remove("data.txt");
  }
}


void loop(){}
