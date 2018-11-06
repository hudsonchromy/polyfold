package dihedralutils;

public class Angular {
  public int id;
  public double tao;
  public double theta;
  private final double BOND_LEN = 3.8;

  public Angular() {
    this.id = 0;
    this.tao = 0;
    this.theta = 0;
  }

  @Override
  public String toString() {
    return "(Theta: " + this.theta + ", Tao: " + this.tao + ")";
  }
}
