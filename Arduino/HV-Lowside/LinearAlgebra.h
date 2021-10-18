class LinearAlgebra
{
  public:
    static void CholeskyDecomposition(double *mat, double *p, int n);
    static void CholeskySolver(const double *mat, const double *p, int n, double *b);
    static void TestCholesky();
  private:
    LinearAlgebra() {}
};
