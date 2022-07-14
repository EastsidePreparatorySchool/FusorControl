// Accumulate - Register a data point. t in microseconds, y in your favorite units.

void SinFit60Hz::Accumulate(unsigned long t, double y)
{
  // t is microseconds
  t = (t-t0) % 50000ul; // 3 full cycles in 1/20 second. We remove only multiples of full cycles.
  long tCycles = (12288ul*t+1562ul)/3125ul;  // number of cycles, expressed in 16 bit fixed point. 
               // Note: (3*65536)/50000 = 12288/3125.
               // Also: 50000*12288 = 614,400,000 which fits in an unsigned long.
  double s = FastTrig::Sin(tCycles);
  double c = FastTrig::Cos(tCycles);

  sum1 += 1.0;
  sumS += s;
  sumC += c;
  sumSS += s*s;
  sumCS += c*s;
  //sumCC += c*c;
  sumY += y;
  sumSY += s*y;
  sumCY += c*y;
}

// SolveFit - Returns best fit parameters for: a*sin(omega*t)+b*cos(omega*t)+c

void SinFit60Hz::SolveFit(double &a, double &b, double &c)
{
  sumCC = sum1 - sumSS;
  double mat[] = {sumSS, sumCS, sumS, sumCS, sumCC, sumC, sumS, sumC, sum1};
  double vec[] = {sumSY, sumCY, sumY};
  double p[3]; // Work area.
  LinearAlgebra::CholeskyDecomposition(mat,p,3);
  LinearAlgebra::CholeskySolver(mat,p,3,vec);
  a = vec[0];
  b = vec[1];
  c = vec[2];
}

// Reset - Zero out the accumulators to begin another fit.
// The phases of the sin and cos are relative to when you called Reset.
// You probably want to record your measurements within a span of a few seconds.
// We don't know how the phase of the arduino clock may drift relative to the line 60 Hz.
// The micros() clock will wrap after about 70 minutes, which will wreck measurements, so don't wait
// that long to start recording after a Reset.

void SinFit60Hz::Reset()
{
  sum1=0.0;
  sumS=0.0;
  sumC=0.0;
  sumSS=0.0;
  sumCS=0.0;
  sumCC=0.0;
  sumY=0.0;
  sumSY=0.0;
  sumCY=0.0;
  t0=micros();
}

// TestFit - Run a simple test case with 40 sample points.

void SinFit60Hz::TestFit()
{
#ifdef TESTING  
  // Mathematica code to generate test input: 0.1*sin(omega*t)+0.4*cos(omega*t)+0.55
  // Timesteps are 5+-1 msec. Noise of +-5 mV added to voltage measurements.
  //   dt = Table[RandomReal[{0.004, 0.006}], {40}];
  //   times = Accumulate[dt];
  //   fitTest =  Transpose[
  //     Table[
  //       {Floor[1000000 t], 0.1 Sin[2 \[Pi] 60 t] + 0.4 Cos[2 \[Pi] 60 t] + 0.55 + RandomReal[{-0.005, 0.005}]}, 
  //     {t, times}]];
  unsigned long t[]=
    {4284, 9693, 15311, 20965, 26184, 30355, 34990, 40317, 45029, 50467, 
    55346, 60759, 65829, 70675, 76425, 81131, 86092, 91298, 96090, 
    101232, 106260, 110738, 115003, 119971, 125934, 129980, 134279,
    139741, 144758, 149373, 154060, 158945, 164306, 169619, 174674,
    179656, 185065, 190929, 196556, 201099};
  double y[] = {0.629674, 0.154185, 
    0.847418, 0.630252, 0.14452, 0.636054, 0.937057, 0.251655, 0.337645, 
    0.963906, 0.466825, 0.222279, 0.897159, 0.678476, 0.159866, 0.744155,
    0.838515, 0.171697, 0.485689, 0.952072, 0.340612, 0.221962, 0.818588,
    0.769814, 0.136869, 0.574455, 0.957821, 0.316342, 0.304758, 0.915746,
    0.669015, 0.137235, 0.725508, 0.812571, 0.168198, 0.522877, 0.926289,
    0.196352, 0.560485, 0.959298};
  int n = sizeof(t)/sizeof(unsigned long);

  unsigned long time0 = micros();
  SinFit60Hz fit;
  unsigned long time1 = micros();
  for (int i=0; i<n; i++)
    fit.Accumulate(t[i]+time0,y[i]);
  unsigned long time2 = micros();
  double a,b,c;
  fit.SolveFit(a,b,c);
  unsigned long time3 = micros();
  Serial.print("FitSin60Hz::TestFit\n");
  Serial.print("a = ");
  Serial.print(a,4);
  Serial.print("\n");
  Serial.print("b = ");
  Serial.print(b,4);
  Serial.print("\n");
  Serial.print("c = ");
  Serial.print(c,4);
  Serial.print("\n");
  Serial.print("times ");
  Serial.print(time1-time0);
  Serial.print(" ");
  Serial.print((time2-time1)/n);
  Serial.print(" ");
  Serial.print(time3-time2);
  Serial.print("\n");
  Serial.print("Amplitude = ");
  Serial.print(sqrt(a*a+b*b),4); // Looking for 0.4123
  Serial.print("\n");
#endif
}
