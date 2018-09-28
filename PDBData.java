public class PDBData {
  int id;
  Point3D ca;

  public PDBData() {
    this.id = 0;
    this.ca = new Point3D();
  }

  @Override
  public String toString() {
    return this.ca.toString();
  }

}
