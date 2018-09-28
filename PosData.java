public class PosData {
  int id;
  double bondLen;
  double tao;
  double theta;

  public PosData() {
    this.id = 0;
    this.bondLen = 3.7;
    this.tao = 0;
    this.theta = 0;
  }

  @Override
  public String toString() {
    return "(Theta: " + this.theta + ", Tao: " + this.tao + ")";
  }
}
