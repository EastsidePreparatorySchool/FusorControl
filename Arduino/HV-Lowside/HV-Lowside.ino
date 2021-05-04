//
// Fusor project code for HV-lowside Arduino
// 
// Adafruit - Mega 2560
//

#include "fusor.h"


// TODO: Wow, a stats class. I should put that into fusor.h so that every Arduino can use it
class stat
{
  public:
  int n;
  float sum;
  float sum2;

  void Reset()
  {
    n = 0;
    sum = 0.0;
    sum2 = 0.0;
  }

  stat()
  {
    Reset();
  }

  void accumulate(float x)
  {
    n++;
    sum += x;
    sum2 += x*x;
  }

  float average()
  {
    return sum / n;
  }

  float variance()
  {
    float v = (sum2 - (sum * sum / n)) / (n-1);
    return v>0.0 ? v : 0.0;
  }

  float standardDeviation()
  {
    return sqrt(variance());
  }
};

stat variacOutput;
stat nstOutput;
stat cwOutput;

#define variacAdcPin 0
#define nstAdcPin 1
#define cwAdcPin 2

long nextDisplayUpdate;

float DividerOffset(float r1, float r2, float rS, float rL, float v1);
float DividerMultiplier(float r1, float r2, float rS, float rL);

float variacOffset = 0.540; // DividerOffset(270.0, 33.0, 8.2e3, 3.3e6, 5.0); // Was 0.543.
float variacMultiplier = DividerMultiplier(270.0, 33.0, 8.2e3, 3.3e6);
float nstOffset = 0.542; // DividerOffset(270.0, 33.0, 8.2e3, 200e6, 5.0); // Was 0.545 but measured 0.542.
float nstMultiplier = DividerMultiplier(270.0, 33.0, 8.2e3, 200e6) / 1000.0; // Make it KV.
float cwOffset = 1.017; // DividerOffset(330.0, 82.0, 10e3, 400e6, 5.0); // Was 0.995, but measured 1.017
float cwMultiplier = DividerMultiplier(330.0, 82.0, 10e3, 400e6) / 1000.0; // Make it KV.
const float adcToVolts = 1.067 / 1023; // This device is not 1.1v. Depends on a particular diode.


void setup(){
  fusorInit("HV-LOWSIDE"); //Fusor device name, variables, num variables
  fusorAddVariable("variac_rms", FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("nst_rms", FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("cw_avg", FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("n", FUSOR_VARTYPE_INT);

  analogReference(INTERNAL1V1); // ADCs compare to 1.1v
  nextDisplayUpdate = millis() + 1000;

  FUSOR_LED_ON();
  delay(200);
  FUSOR_LED_OFF();
}


void loop() {
  fusorLoop();
  updateAll();
  
  delay(5);
}

void updateAll() {
  // Read the variac voltage
  // The variac divider uses 8.2k and 3.3m resistors. Scale by: (3.3m + 8.2k)/(8.2k)(1.1 / 1023) = 0.43381
  //  float variacReading = analogRead(variacAdcPin) * 0.43381;
  float variacReading = (analogRead(variacAdcPin)*adcToVolts - variacOffset) * variacMultiplier;
  variacOutput.accumulate(variacReading);  

  // Read the NST voltage
  // The NST divider uses 8.2k and 200m resistors. Scale by: (200m + 8.2k)/(8.2k)(1.1 / 1023) = 26.227
  //  float nstReading = analogRead(nstAdcPin) * 0.026227; // Make it KV
  float nstReading = (analogRead(nstAdcPin)*adcToVolts - nstOffset) * nstMultiplier; // In KV.
  nstOutput.accumulate(nstReading);

  // Read the CW voltage
  float cwReading = (analogRead(cwAdcPin)*adcToVolts - cwOffset) * cwMultiplier; // In KV.
  cwOutput.accumulate(cwReading);

  if (millis() > nextDisplayUpdate)
  {
    nextDisplayUpdate += 500;
    UpdateDisplay(); // Also clears counters
  }
}

void UpdateDisplay()
{
  float variacAverage = variacOutput.average();
  float variacRMS = variacOutput.standardDeviation();
  int variacN = variacOutput.n;
  variacOutput.Reset();
  
  float nstAverage = nstOutput.average();
  float nstRMS = nstOutput.standardDeviation();
  int nstN = nstOutput.n;
  nstOutput.Reset();

  float cwAverage = cwOutput.average();
  float cwRMS = cwOutput.standardDeviation();
  cwOutput.Reset();

  fusorSetFloatVariable("variac_rms", variacRMS);
  fusorSetFloatVariable("nst_rms", nstRMS);
  fusorSetFloatVariable("cw_avg", cwAverage);
  fusorSetIntVariable("n", variacN);
}

/*
We measure high voltages with the assistance of a divider circuit.
There is a central node where three resistors connect.
R1 connects the node with a supply voltage, typically +5V.
R2 connects the node to ground.
R3 is the sum of a small resistor RS and a larger resistor RL, and it connects
the node to the high voltage we wish to measure.
The smaller voltage measured by the ADC is over R2+RS.
 */


float DividerOffset(float r1, float r2, float rS, float rL, float v1)
{
  float r3 = rS + rL;
  float rr123 = r1*r2 + r1*r3 + r2*r3;
  return v1*r2*rL/rr123;
}

float DividerMultiplier(float r1, float r2, float rS, float rL)
{
  float r3 = rS + rL;
  float rr123 = r1*r2 + r1*r3 + r2*r3;
  float rr12s = r1*r2 + r1*rS + r2*rS;
  return rr123 / rr12s;
}
