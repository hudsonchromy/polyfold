package dihedralutils;

public class Cartesian {
  public int id;
  public Point pos;

  public Cartesian() {
    this.id = 0;
    this.pos = new Point();
  }

  @Override
  public String toString() {
    return this.pos.toString();
  }
}
