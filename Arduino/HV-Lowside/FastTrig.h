// FastTrig - Uses integer arithmetic to provide close approximations to sin() and cos().

class FastTrig
{
public:  
  static float Cos(long x); // x is 16 bit fixed point number of rotations.
  static float Sin(long x)
  {
    return Cos(x + 3*16384L); // Just add 0.11b to the fixed point argument, moving 3 quadrants.
  }
  static void Test();  
private:
  FastTrig() // It's a static class; never call the constructor.
  {}
  static const long b; // Magic number for InnerCos.
  static long InnerCos(long x); // x is 14 bit positive fraction of a quadrant. (0 <= x <= 16384)
};
