public class Point3D {
  double x;
  double y;
  double z;

  public Point3D() {
    this.x = 0;
    this.y = 0;
    this.z = 0;
  }

  @Override
  public String toString() {
    return "(" + this.x + " " + this.y + " " + this.z + ")";
  }
}
