package polyfold;

public class Score {
  public static double meanSquaredError(double[][] a, double[][] b) {
    double error = 0;
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a[0].length; j++) {
        double diffSquared = Math.pow(a[i][j] - b[i][j], 2);
        error += diffSquared;
      }
    }
    return error;
  }

  public static double sigmoid(double error) {
    return 1.0 / (1.0 + Math.pow(Math.E, -error));
  }

  public static double relu(double error) {
    return Math.max(0, error);
  }
}
