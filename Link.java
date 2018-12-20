import javafx.scene.*;
import javafx.scene.shape.*;
import javafx.scene.transform.*;
import javafx.geometry.Point3D;
import dihedralutils.Point;
import dihedralutils.DihedralUtility;
import java.util.*;

public class Link extends Group {
  private final double NODE_RADIUS = 0.9;
  private final double ROD_RADIUS = 0.15;
  private final double ROD_HEIGHT = DihedralUtility.BOND_LEN;

  public Sphere node;
  public Cylinder rod = new Cylinder(0, 0);
  // amino acid
  public char aa;
  // secondary structure
  public char ss;
  // id for referencing
  public int id;
  // used for rotating about a given axis
  public Rotate rotation;

  // default constructor
  public Link(double x, double y, double z, boolean isEnd) {
    super();
    this.node = new Sphere(NODE_RADIUS);
    if (!isEnd) this.rod = initRod(ROD_RADIUS, ROD_HEIGHT);
    // note the rotations axis will be modifies as needed during runtime
    this.rotation = initRotation(Rotate.Z_AXIS);

    getChildren().addAll(node, rod);
    getTransforms().add(rotation);
    setCenter(x, y, z);
  }

  // initialization if amino acid type and secondary structure provided
  public Link(double x, double y, double z, boolean isEnd, char aa, char ss) {
    super();
    this.node = new Sphere(NODE_RADIUS);
    if (!isEnd) this.rod = initRod(ROD_RADIUS, ROD_HEIGHT);
    this.rotation = initRotation(Rotate.Z_AXIS);
    this.aa = aa;
    this.ss = ss;

    getChildren().addAll(node, rod);
    getTransforms().add(rotation);
    setCenter(x, y, z);
  }

  private Cylinder initRod(double radius, double height) {
    Cylinder rod = new Cylinder(radius, height);
    rod.setTranslateY(0.5 * DihedralUtility.BOND_LEN);
    return rod;
  }

  private Rotate initRotation(Point3D axis) {
    Rotate plane = new Rotate();
    plane.pivotXProperty().bind(node.translateXProperty());
    plane.pivotYProperty().bind(node.translateYProperty());
    plane.pivotZProperty().bind(node.translateZProperty());
    plane.setAxis(axis);
    return plane;
  }

  public void setCenter(double xF, double yF, double zF) {
    double x0, y0, z0;
    x0 = node.getTranslateX();
    y0 = node.getTranslateY();
    z0 = node.getTranslateZ();
    double x, y, z;
    x = xF - x0;
    y = yF - y0;
    z = zF - z0;
    setTranslateX(x);
    setTranslateY(y);
    setTranslateZ(z);
  }

  public Point getCenter() {
    return new Point(getTranslateX(), getTranslateY(), getTranslateZ());
  }

  public void rotateRod(Link other) {
    double x0, y0, z0, xF, yF, zF;
    xF = getTranslateX();
    yF = getTranslateY();
    zF = getTranslateZ();
    x0 = other.getTranslateX();
    y0 = other.getTranslateY();
    z0 = other.getTranslateZ();

    double x = xF - x0;
    double y = yF - y0;
    double z = zF - z0;
    // represents X Axis
    Point p0 = new Point(0, 1, 0);
    // represents vector between two spheres
    Point p1 = new Point(x, y, z);
    // cross product returns a point orthogonal to both - our rotation axis
    Point axis = DihedralUtility.getCrossProd(p0, p1);
    double r = Math.hypot(x, z);
    double phi = Math.toDegrees(Math.atan2(r, y));
    rotation.setAxis(new Point3D(axis.x, axis.y, axis.z));
    rotation.setAngle(180 + phi);
  }
}
