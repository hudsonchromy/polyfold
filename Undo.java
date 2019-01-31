public class Undo {
  int id;
  char angleType;
  double angle;

  public Undo(int id, char angleType, double angle) {
    this.id = id;
    this.angleType = angleType;
    this.angle = angle;
  }

  public int getID() {
    return id;
  }

  public char getAngleType() {
    return angleType;
  }

  public double getAngle() {
    return angle;
  }
  public String toString() {
    return("id " + id + "\nangle type " + angleType + "\nangle " + angle);
  }
}
