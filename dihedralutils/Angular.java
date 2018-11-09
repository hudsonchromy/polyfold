package dihedralutils;

public class Angular {
  public int id;
  public double tao;
  public double theta;
  public char aa;
  public char ss;
  private final double BOND_LEN = 3.8;

  public Angular() {
    this.id = 0;
    this.tao = 0;
    this.theta = 0;
    this.aa = 0;
    this.ss = 0;
  }

  @Override
  public String toString() {
    return "(Theta: " + this.theta + ", Tao: " + this.tao + ")";
  }
}
