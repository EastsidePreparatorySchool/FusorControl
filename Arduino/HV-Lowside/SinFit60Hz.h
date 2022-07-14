// SinFit60Hz - Finds a best-fit sine curve to accumulated data points.

class SinFit60Hz
{
  double sum1=0.0;
  double sumS=0.0;
  double sumC=0.0;
  double sumSS=0.0;
  double sumCS=0.0;
  double sumCC=0.0;
  double sumY=0.0;
  double sumSY=0.0;
  double sumCY=0.0;
  unsigned long t0 = micros();

public:
  SinFit60Hz()
  {}
  void Accumulate(unsigned long t, double y);
  void SolveFit(double &a, double &b, double &c);
  void Reset();
  static void TestFit();
};
