package dihedralutils;

public class DihedralUtility {
  public static final double BOND_LEN = 3.8;
  public static final double PI = Math.PI;

  public static Angular[] carts2Angles(Cartesian[] carts) {
    Angular[] angles = new Angular[carts.length];

    // virtual N terminus
    Point N = new Point();
    N.x = carts[0].ca.x - Math.cos(PI) * BOND_LEN;
    N.y = carts[0].ca.y - Math.sin(PI) * BOND_LEN;
    N.z = carts[0].ca.z;
    // virtual C terminus
    Point C = new Point();
    C.x = carts[carts.length-1].ca.x + Math.cos(PI) * BOND_LEN;
    C.y = carts[carts.length-1].ca.y + Math.sin(PI) * BOND_LEN;
    C.z = carts[carts.length-1].ca.z;

    Angular a;
    for (int i = 0; i < carts.length; i++) {
      a = new Angular();
      a.id = carts[i].id;
      // tao
      if (i == 0 || i == carts.length-1 || i == carts.length-2) {
        a.tao = 2*PI;
      }
      else {
        a.tao = getDihedral(carts[i-1].ca, carts[i].ca, carts[i+1].ca, carts[i+2].ca);
      }
      // theta
      if (i == 0 || i == carts.length-1) {
        a.theta = 2*PI;
      }
      else {
        a.theta = getAngle(carts[i-1].ca, carts[i].ca, carts[i+1].ca);
      }
      angles[i] = a;
    }
    return angles;
  }

  public static Cartesian[] angles2Carts(Angular[] angles) {
    Cartesian[] carts = new Cartesian[angles.length];
    Cartesian c = new Cartesian();

    // virtual N terminus
    Point N = new Point();
    N.x = 0.0 - Math.cos(PI) * BOND_LEN;
    N.y = 0.0 - Math.sin(PI) * BOND_LEN;
    N.z = 0.0;
    // first node at origin
    c.id = angles[0].id;
    c.ca.x = 0.0;
    c.ca.y = 0.0;
    c.ca.z = 0.0;
    carts[0] = c;
    // second node at bond length along x axis
    c = new Cartesian();
    c.id = angles[1].id;
    c.ca.x = BOND_LEN;
    c.ca.y = 0.0;
    c.ca.z = 0.0;
    carts[1] = c;
    // third node at bond angle and bond length
    c = new Cartesian();
    c.id = angles[2].id;
    setCoordinate(N, carts[0].ca, carts[1].ca, c.ca, angles[1].tao, angles[1].theta, BOND_LEN);
    carts[2] = c;

    for (int i = 3; i < angles.length; i++) {
      c = new Cartesian();
      c.id = angles[i].id;
      setCoordinate(carts[i-3].ca, carts[i-2].ca, carts[i-1].ca, c.ca, angles[i-2].tao, angles[i-1].theta, BOND_LEN);
      carts[i] = c;
    }
    return carts;
  }


  public static void setCoordinate(Point c0, Point c1, Point c2, Point c3, double alpha, double tao, double normw) {
    double  u1, u2, u3, v1, v2, v3, norm;
    double pvuv1, pvuv2, pvuv3, pvvuv1, pvvuv2, pvvuv3;
    double nsa, nca, nct;
    u1 = (c1.x - c0.x);
    u2 = (c1.y - c0.y);
    u3 = (c1.z - c0.z);
    v1 = (c2.x - c1.x);
    v2 = (c2.y - c1.y);
    v3 = (c2.z - c1.z);
    norm = Math.sqrt(v1 * v1 + v2 * v2 + v3 * v3);
    v1 /=  norm;
    v2 /=  norm;
    v3 /=  norm;
    pvuv1 = u2 * v3 - u3 * v2;
    pvuv2 = u3 * v1 - u1 * v3;
    pvuv3 = u1 * v2 - u2 * v1;
    norm = Math.sqrt(pvuv1 * pvuv1 + pvuv2 * pvuv2 + pvuv3 * pvuv3);
    pvuv1 /= norm;
    pvuv2 /= norm;
    pvuv3 /= norm;
    pvvuv1 = v2 * pvuv3 - v3 * pvuv2;
    pvvuv2 = v3 * pvuv1 - v1 * pvuv3;
    pvvuv3 = v1 * pvuv2 - v2 * pvuv1;
    norm = Math.sqrt(pvvuv1 * pvvuv1 + pvvuv2 * pvvuv2 + pvvuv3 * pvvuv3);
    pvvuv1 /= norm;
    pvvuv2 /= norm;
    pvvuv3 /= norm;
    nca = Math.cos(alpha);
    nsa = Math.sin(alpha);
    nct = Math.tan(tao - PI/2);
    u1 = nca * (-pvvuv1) + nsa * pvuv1 + v1 * nct;
    u2 = nca * (-pvvuv2) + nsa * pvuv2 + v2 * nct;
    u3 = nca * (-pvvuv3) + nsa * pvuv3 + v3 * nct;
    norm = Math.sqrt(u1 * u1 + u2 * u2 + u3 * u3);
    u1 = u1 * normw/norm;
    u2 = u2 * normw/norm;
    u3 = u3 * normw/norm;
    c3.x = u1 + c2.x;
    c3.y = u2 + c2.y;
    c3.z = u3 + c2.z;
  }

  public static double getDistance(Point p1, Point p2) {
    return Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2) + Math.pow(p1.z - p2.z, 2);
  }

  public static double getDihedral(Point p1, Point p2, Point p3, Point p4) {
    Point q, r, s, t, u, v;
    Point z = new Point();
    z.x = 0.0;
    z.y = 0.0;
    z.z = 0.0;
    double acc, w;
    q = getDifference(p1, p2);
    r = getDifference(p3, p2);
    s = getDifference(p4, p3);
    t = getCrossProd(q, r);
    u = getCrossProd(s, r);
    v = getCrossProd(u, t);
    w = getDotProd(v, r);
    acc = getAngle(t, z, u);
    if (w < 0) {
      acc = -acc;
    }
    return acc;
  }

  public static Point getDifference(Point p1, Point p2) {
    Point p = new Point();
    p.x = p2.x - p1.x;
    p.y = p2.y - p1.y;
    p.z = p2.z - p1.z;
    return p;
  }

  public static Point getCrossProd(Point p1, Point p2) {
    Point p = new Point();
    p.x = p1.y * p2.z - p1.z * p2.y;
    p.y = p1.z * p2.x - p1.x * p2.z;
    p.z = p1.x * p2.y - p1.y * p2.x;
    return p;
  }

  public static double getDotProd(Point p1, Point p2) {
    return (p1.x * p2.x + p1.y * p2.y + p1.z * p2.z);
  }

  public static double getAngle(Point p1, Point p2, Point p3) {
    double acc = 0.0;
    acc = (p2.x - p1.x) * (p2.x - p3.x) + (p2.y - p1.y) * (p2.y - p3.y) + (p2.z - p1.z) * (p2.z - p3.z);
    double d1 = BOND_LEN;
    double d2 = BOND_LEN;

    acc /= (d1 * d2);
    if (acc > 1.0) {
      acc = 1.0;
    }
    else if (acc < -1.0) {
      acc = -1.0;
    }
    acc = Math.acos(acc);
    return acc;
  }
}


