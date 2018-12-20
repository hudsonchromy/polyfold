package dihedralutils;

public class Cartesian {
  public int id;
  public Point ca;

  public Cartesian() {
    this.id = 0;
    this.ca = new Point();
  }

  @Override
  public String toString() {
    return this.ca.toString();
  }
}
