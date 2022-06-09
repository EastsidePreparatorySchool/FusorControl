// CholeskyDecomposition - Factors a positive definite square matrix.
// On return, the matrix has been replaced with a lower triangular matrix.
// What would be the diagonal elements are returned in p.

void LinearAlgebra::CholeskyDecomposition(double *mat, double *p, int n)
{
  for (int i=0, ni=0; i<n; i++, ni+=n)
  {
    double e;
    for (int j=i, nj=ni; j<n; j++, nj+=n)
    {
      double sum = mat[ni+j];
      for (int k=i-1; k>=0; k--)
        sum -= mat[ni+k]*mat[nj+k];
      if (i==j)
        e = sqrt(abs(sum));
      else
        mat[nj+i] = sum/e;
    }
    p[i] = e;
  }
}

// Solves the inverse problem mat.x = b.
// Input is the decomposed matrix and p vector.
// The b vector is replaced with the answer x.

void LinearAlgebra::CholeskySolver(const double *mat, const double *p, int n, double *b)
{
  for (int i=0; i<n; i++)
  {
    double sum = b[i];
    for (int k=i-1; k>=0; k--)
      sum -= mat[n*i+k]*b[k];
    b[i] = sum/p[i];
  }
  for (int i=n-1; i>=0; i--)
  {
    double sum = b[i];
    for (int k=i+1; k<n; k++)
      sum -= mat[k*n+i]*b[k];
    b[i] = sum/p[i];
  }
}

void LinearAlgebra::TestCholesky()
{
#ifdef TESTING  
  double a[9] = {
    5.0, 2.0, 3.0,
    2.0, 7.0, 5.0,
    3.0, 5.0, 6.0
  };
  double p[3];
  char buf[15];
  CholeskyDecomposition(a, p, 3);
  Serial.print("Test Cholesky\n");
  for (int i=0; i<3; i++)
  {
    for (int j=0; j<3; j++)
    {
      double e = (j<i) ?  a[3*i+j] : (j==i) ? p[i] : 0.0;
      Serial.print(dtostrf(e,10,5,buf));
    }
    Serial.print("\n");
  }
  double b[3] = {1.0, 2.0, 3.0};
  CholeskySolver(a, p, 3, b);
  Serial.print("Solution\n");
  for (int i=0; i<3; i++)
  {
    Serial.print(dtostrf(b[i],10,5,buf));
  }
  Serial.print("\n");
#endif
}
