//
// Fusor project code for HV-lowside Arduino
// 
// Adafruit - Mega 2560
//

#include "fusor.h"
#include "FastTrig.h"
#include "LinearAlgebra.h"
#include "SinFit60Hz.h"

#define variacAdcPin 4
#define nstAdcPin 5
#define cwAdcPin 6
#define cwCurrentAdcPin 1

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

SinFit60Hz variacOutputFitter = SinFit60Hz();
SinFit60Hz nstOutputFitter = SinFit60Hz();
stat cwOutput;
stat cwCurrent;

long nextDisplayUpdate;

const int captureCycles = 6;
const long captureTimeUs = (long)(1.0 / 60.0 * captureCycles * 1000000.0);

const float VARIAC_R1 = 10.83 * 1000;
const float VARIAC_R2 = 88.6 * 1000;
const float VARIAC_R3 = 3.23 * 1000000;

const float NST_R1 = 10.82 * 1000;
const float NST_R2 = 89.3 * 1000;
const float NST_R3 = 200 * 1000000;


float DividerMultiplier(float r1, float r2, float rS, float rL)
{
  float r3 = rS + rL;
  float rr123 = r1*r2 + r1*r3 + r2*r3;
  float rr12s = r1*r2 + r1*rS + r2*rS;
  return rr123 / rr12s;
}

float cwOffset = 1.017; // DividerOffset(330.0, 82.0, 10e3, 400e6, 5.0); // Was 0.995, but measured 1.017
float cwMultiplier = DividerMultiplier(330.0, 82.0, 10e3, 400e6) / 1000.0; // Make it KV.
const float currentResistor = 100; // 100 Ohm
const float adcToVolts = 1.067 / 1023; // This device is not 1.1v. Depends on a particular diode.

float f(float r1, float r2, float r3){
  return (r1*r2)/(r1*r3+r1*r2+r2*r3);
}

float v(float a, float r1, float r2, float r3){
  return (((a*1.1)/1023) - 5*f(r1,r3,r2))/f(r1,r2,r3);
}

float rms(float a, float b){
  return sqrt(a*a+b*b)*0.707107;
}

void setup(){
  fusorInit("HV-LOWSIDE"); //Fusor device name, variables, num variables
  fusorAddVariable("variac_rms", FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("nst_rms", FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("cw_avg", FUSOR_VARTYPE_FLOAT);
  fusorAddVariable("cwc_avg", FUSOR_VARTYPE_FLOAT);
  
  analogReference(INTERNAL1V1); // ADCs compare to 1.1v
  nextDisplayUpdate = millis() + 1000;

  FUSOR_LED_ON();
  delay(200);
  FUSOR_LED_OFF();
}


void loop() {
  fusorLoop();
  long endTime = micros()+captureTimeUs;
  do {
    updateAll();
  } while (micros() < endTime);
  UpdateDisplay();
}

void updateAll() {
  float variacReading = analogRead(variacAdcPin);
  variacOutputFitter.Accumulate(micros(), v(variacReading, VARIAC_R1, VARIAC_R2, VARIAC_R3));   

  float nstReading = analogRead(nstAdcPin);
  nstOutputFitter.Accumulate(micros(), v(nstReading, NST_R1, NST_R2, NST_R3)/1000); //in KV 

  float cwReading = analogRead(cwAdcPin) / 1023.0;
  cwOutput.accumulate((cwReading*1.1 - cwOffset) * cwMultiplier); // In KV.

  // Read the CW current
  // Measured as voltage over 100 Ohm resistor
  float cwCurrentReading = analogRead(cwCurrentAdcPin) / 1023.0; // readConstantTime(cwCurrentAdcPin, 0, 1000.0/currentResistor); // in mA
  cwCurrent.accumulate((cwCurrentReading*1.1) * (1000.0/currentResistor));
}

void UpdateDisplay()
{
  double variacA, variacB, variacC;
  variacOutputFitter.SolveFit(variacA, variacB, variacC);
  
  float variacRMS = rms(variacA, variacB);
  variacOutputFitter.Reset();
  
  double nstA, nstB, nstC;
  nstOutputFitter.SolveFit(nstA, nstB, nstC);
  
  float nstRMS = rms(nstA, nstB);
  nstOutputFitter.Reset();

  float cwAverage = cwOutput.average();
  float cwcAverage = cwCurrent.average();
  cwOutput.Reset();
  cwCurrent.Reset();

  fusorSetFloatVariable("variac_rms", variacRMS);
  fusorSetFloatVariable("nst_rms", nstRMS);
  fusorSetFloatVariable("cw_avg", cwAverage);
  fusorSetFloatVariable("cwc_avg", cwcAverage);
}
