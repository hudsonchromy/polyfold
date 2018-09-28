import java.util.*;

public class DihedralUtility {
  public static final double BOND_LENGTH = 3.8;
  public static final double PI = Math.PI;

  public static PosData[] pdb2Pos(PDBData[] pdb) {
    PosData[] pos = new PosData[pdb.length];

    // virtual N terminus
    Point3D N = new Point3D();
    N.x = pdb[0].ca.x - Math.cos(PI) * BOND_LENGTH;
    N.y = pdb[0].ca.y - Math.sin(PI) * BOND_LENGTH;
    N.z = pdb[0].ca.z;
    // virtual C terminus
    Point3D C = new Point3D();
    C.x = pdb[pdb.length-1].ca.x + Math.cos(PI) * BOND_LENGTH;
    C.y = pdb[pdb.length-1].ca.y + Math.sin(PI) * BOND_LENGTH;
    C.z = pdb[pdb.length-1].ca.z;

    PosData posData;
    for (int i = 0; i < pdb.length; i++) {
      posData = new PosData();
      posData.id = pdb[i].id;
      posData.bondLen = BOND_LENGTH;
      // tao
      if (i == 0 || i == pdb.length-1 || i == pdb.length-2) {
        posData.tao = 2*PI;
      }
      else {
        posData.tao = getDihedral(pdb[i-1].ca, pdb[i].ca, pdb[i+1].ca, pdb[i+2].ca);
      }
      // theta
      if (i == 0 || i == pdb.length-1) {
        posData.theta = 2*PI;
      }
      else {
        posData.theta = getAngle(pdb[i-1].ca, pdb[i].ca, pdb[i+1].ca);
      }
      pos[i] = posData;
    }
    return pos;
  }

  public static PDBData[] pos2pdb(PosData[] pos) {
    PDBData[] pdb = new PDBData[pos.length];
    PDBData pdbData = new PDBData();

    // virtual N terminus
    Point3D N = new Point3D();
    N.x = 0.0 - Math.cos(PI) * BOND_LENGTH;
    N.y = 0.0 - Math.sin(PI) * BOND_LENGTH;
    N.z = 0.0;
    // first node at origin
    pdbData.id = pos[0].id;
    pdbData.ca.x = 0.0;
    pdbData.ca.y = 0.0;
    pdbData.ca.z = 0.0;
    pdb[0] = pdbData;
    // second node at bond length along x axis
    pdbData = new PDBData();
    pdbData.id = pos[1].id;
    pdbData.ca.x = pos[1].bondLen;
    pdbData.ca.y = 0.0;
    pdbData.ca.z = 0.0;
    pdb[1] = pdbData;
    // third node at bond angle and bond length
    pdbData = new PDBData();
    pdbData.id = pos[2].id;
    setCoordinate(N, pdb[0].ca, pdb[1].ca, pdbData.ca, pos[1].tao, pos[1].theta, pos[1].bondLen);
    pdb[2] = pdbData;

    for (int i = 3; i < pos.length; i++) {
      pdbData = new PDBData();
      pdbData.id = pos[i].id;
      setCoordinate(pdb[i-3].ca, pdb[i-2].ca, pdb[i-1].ca, pdbData.ca, pos[i-2].tao, pos[i-1].theta, pos[i-1].bondLen);
      pdb[i] = pdbData;
    }
    return pdb;
  }


  // TODO Get clarity on this method
  public static void setCoordinate(Point3D c0, Point3D c1, Point3D c2, Point3D c3, double alpha, double tao, double normw) {
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

  public static double getDistance(Point3D p1, Point3D p2) {
    return Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2) + Math.pow(p1.z - p2.z, 2);
  }

  public static double getDihedral(Point3D p1, Point3D p2, Point3D p3, Point3D p4) {
    Point3D q, r, s, t, u, v;
    Point3D z = new Point3D();
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

  public static Point3D getDifference(Point3D p1, Point3D p2) {
    Point3D p = new Point3D();
    p.x = p2.x - p1.x;
    p.y = p2.y - p1.y;
    p.z = p2.z - p1.z;
    return p;
  }

  public static Point3D getCrossProd(Point3D p1, Point3D p2) {
    Point3D p = new Point3D();
    p.x = p1.y * p2.z - p1.z * p2.y;
    p.y = p1.z * p2.x - p1.x * p2.z;
    p.z = p1.x * p2.y - p1.y * p2.x;
    return p;
  }

  public static double getDotProd(Point3D p1, Point3D p2) {
    return (p1.x * p2.x + p1.y * p2.y + p1.z * p2.z);
  }

  public static double getAngle(Point3D p1, Point3D p2, Point3D p3) {
    double acc = 0.0;
    acc = (p2.x - p1.x) * (p2.x - p3.x) + (p2.y - p1.y) * (p2.y - p3.y) + (p2.z - p1.z) * (p2.z - p3.z);
    double d1 = BOND_LENGTH;
    double d2 = BOND_LENGTH;

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


