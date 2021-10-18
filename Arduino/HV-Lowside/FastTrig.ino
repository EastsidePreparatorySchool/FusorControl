// x is a 16.16 bit fixed point number of rotations. (Meaning 16 binary digits are to the right of the 'decimal' point.)

float FastTrig::Cos(long x) 
{
  x = x & 65535; // Integer number of rotations is irrelevant.
  long z = x & 16383L; // 14 bit fixed point fraction of quadrant.
  int q = x >> 14; // Quadrant number.

  if (q & 1) z = 16384L - z;
  long r = InnerCos(z);
  if (q==1 || q==2) r = -r;
  return r * 1.52588e-5f; // 1.0/65536.
}

// Fast approximation to cosine. Accurate to +-0.0009.
// Input x is 14 bit positive fraction of a quadrant. (0 <= x <= 16384)
// Output is 16 bit fixed point.

const long FastTrig::b = 14682; // 2^16 * 0.22402317

long FastTrig::InnerCos(long x)
{
  // Compute: (1-x2)*(1-b*x2)
  // Note that x2 represents x^2. 
  // When x^2=0, the formula returns 1.0. When x^2 is 1, being a full quadrant (i.e. pi/2), 
  // the first factor guarantees a 0.0 result.
  // In between, we choose a magic number b which minimizes the absolute error.
  // (For even better accuracy, make b a polynomial in x^2.)
  long x2 = (x*x+4096L)>>13; // 15 bit fixed.
  long right = 32768L - ((b*x2+32768L)>>16);
  long left = 32768L - x2;
  return (right*left+8192L)>>14; // Returns 16 bit fixed.
}

void FastTrig::Test()
{
#ifdef TESTING  
  char buf[30];
  char cbuf[15];
  char sbuf[15];
  Serial.print("Test trig\n");
  unsigned long timing = 0;
  int count = 0;
  for (int angle=0; angle<=720; angle+=10)
  {
    long rotations = angle*65536L/360;
    timing -= micros();
    float c = Cos(rotations);
    float s = Sin(rotations);
    timing += micros();
    count += 2;
    dtostrf(c,10,5,cbuf);
    dtostrf(s,10,5,sbuf);
    sprintf(buf,"%3d %s %s\n",angle,sbuf,cbuf);
    Serial.print(buf);
  }
  long avgTime = timing/count;
  Serial.print("Avg trig time = ");
  Serial.print(avgTime);
  Serial.print("\n");
#endif
}
