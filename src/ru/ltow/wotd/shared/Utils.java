package ru.ltow.wotd;

import java.util.Arrays;
import java.util.ArrayList;

/*static class for coordinate operations and world spatial constants*/
public class Utils {
  public static int X = 0;
  public static int Y = 1;
  public static int Z = 2;

  private static void vcheck(float[] a, float[] b) {
    int d = ClientConstants.DIMS;
    if(a.length != b.length || a.length != d || b.length != d) 
    throw new IllegalArgumentException("wrong dims or lengths differ");
  }

  private static void vcheck(int[] a, int[] b) {vcheck(Utils.itof(a), Utils.itof(b));}

  public static float[] itof(int[] a) {
    float[] b = new float[a.length];
    for(int i = 0; i < b.length; i++) b[i] = (float) a[i];
    return b;
  }

  public static int[] ftoi(float[] a) {
    int[] b = new int[a.length];
    for(int i = 0; i < b.length; i++) b[i] = (int) a[i];
    return b;
  }

  public static float[] vcross(float[] a, float[] b) {
    Utils.vcheck(a, b);
    return new float[]{
      a[Y]*b[Z] - a[Z]*b[Y],
      a[Z]*b[X] - a[X]*b[Z],
      a[X]*b[Y] - a[Y]*b[X]
    };
  }

  public static int[] vcross(int[] a, int[] b) {
    Utils.vcheck(a, b);
    return new int[]{
      a[Y]*b[Z] - a[Z]*b[Y],
      a[Z]*b[X] - a[X]*b[Z],
      a[X]*b[Y] - a[Y]*b[X]
    };
  }

  public static float vdot(float[] a, float[] b) {
    Utils.vcheck(a, b);
    float c = 0;
    for(int i = 0; i < a.length; i++) {c += a[i]*b[i];}
    return c;
  }

  public static int vdot(int[] a, int[] b) {
    Utils.vcheck(a, b);
    int c = 0;
    for(int i = 0; i < a.length; i++) {c += a[i]*b[i];}
    return c;
  }

  public static float[] vnmul(float[] a, float n) {
    float [] b = a.clone();
    for(int i = 0; i < b.length; i++) {
      b[i] *= n;
    }
    return b;
  }

  public static int[] vnmul(int[] a, int n) {
    int [] b = a.clone();
    for(int i = 0; i < b.length; i++) {
      b[i] *= n;
    }
    return b;
  }

  public static float[] vndiv(float[] a, float n) {
    float[] b = a.clone();
    for(int i = 0; i < b.length; i++) {
      b[i] /= n;
    }
    return b;
  }

  public static int[] vndiv(int[] a, int n) {
    int[] b = a.clone();
    for(int i = 0; i < b.length; i++) {
      b[i] /= n;
    }
    return b;
  }

  public static int[] vnadd(int[] a, int n) {
    int[] b = a.clone();
    for(int i = 0; i < b.length; i++) {
      b[i] += n;
    }
    return b;
  }

  /*public static int[] vndivmod(int[] a, int n) {
    int[] b = a.clone();
    for(int i = 0; i < b.length; i++) {
      b[i] %= n;
    }
    return b;
  }*/

  public static float[] vadd(float[] a, float[] b) {
    boolean yn = (a.length > b.length);
    float[] c = yn ? a.clone() : b.clone();

    for(int i = 0; i < (yn ? b.length : a.length); i++) {
      c[i] += yn ? b[i] : a[i];
    }
    return c;
  }

  public static int[] vadd(int[] a, int[] b) {
    boolean yn = (a.length > b.length);
    int[] c = yn ? a.clone() : b.clone();

    for(int i = 0; i < (yn ? b.length : a.length); i++) {
      c[i] += yn ? b[i] : a[i];
    }
    return c;
  }

  public static float[] vsub(float[] a, float[] b) {
    float[] c = b.clone();
    for(int i = 0; i < c.length; i++) {
      c[i] *= -1;
    }
    return Utils.vadd(a, c);
  }

  public static int[] vsub(int[] a, int[] b) {
    int[] c = b.clone();
    for(int i = 0; i < c.length; i++) {
      c[i] *= -1;
    }
    return Utils.vadd(a, c);
  }

  public static int[] vmul(int[] a, int[] b) {
    boolean yn = (a.length > b.length);
    int[] c = yn ? a.clone() : b.clone();

    for(int i = 0; i < (yn ? b.length : a.length); i++) {
      c[i] *= yn ? b[i] : a[i];
    }
    return c;
  }

  public static float[] vnorm(float[] a) {
    return Utils.vndiv(a, Utils.vlen(a));
  }

  public static float vlen(float[] a) {
    return (float) Math.sqrt((double) Utils.vlen2(a));
  }

  public static int vlen(int[] a) {
    return (int) Math.sqrt((double) Utils.vlen2(a));
  }

  public static float vlen2(float[] a) {
    float b = 0;
    for(int i = 0; i < a.length; i++) {
       b += a[i] * a[i];
    }
    return b;
  }

  public static int vlen2(int[] a) {
    int b = 0;
    for(int i = 0; i < a.length; i++) {
       b += a[i] * a[i];
    }
    return b;
  }

  public static int[] wrapCoords(int[] a) {return Utils.wrap(a, 0, WotdConstants.COORDS_MAX);}
  public static int[] wrapLocation(int[] a) {return Utils.wrap(a, 0, WotdConstants.SEC_SIZE);}

  private static int[] wrap(int[] a, int min, int max) {
    int[] b = new int[a.length];
    for(int i = 0; i < b.length; i++) {
      b[i] =
        (a[i] < min) ? a[i] + max:
        (a[i] < max) ? a[i] : a[i] - max;
    }

    //wrap only xy
    if(b.length > ClientConstants.DIMS-1) b[Z] = a[Z];

    return b;
  }

  private static int wrap(int a, int min, int max) {
    return Utils.wrap(new int[]{a}, min, max)[X];
  }

  public static int[] vdistanceCoords(int[] a, int[] b) {
    return vdistance(a, b, WotdConstants.COORDS_MAX);
  }

  public static int[] vdistanceLocation(int[] a, int[] b) {
    return vdistance(a, b, WotdConstants.SEC_SIZE);
  }

  //distance vector for looped xy
  public static int[] vdistance(int[] a, int[] b, int cmax) {
    int[] c = Utils.vsub(a, b);
    int[] vdistance = new int[c.length];
    for(int i = 0; i < vdistance.length; i++) {
      vdistance[i] =
        (c[i] >=  cmax/2) ? c[i] - cmax :
        (c[i] <  -cmax/2) ? c[i] + cmax : c[i];
    }  
    return vdistance;
  }

  //squared distance, having in mind that the world is looped on xy plane
  public static int distanceCoords2(int[] a, int[] b) {
    return Utils.vlen2(Utils.vdistanceCoords(a, b));
  }

  public static int distanceCoordsXY2(int[] a, int[] b) {
    return Utils.vlen2(Utils.vdistanceCoords(
      Arrays.copyOfRange(a, X, Z),
      Arrays.copyOfRange(b, X, Z)
    ));
  }

  //location index, sector contains locations
  public static long locationIdx(int[] i) {
    return i[X] + i[Y] * WotdConstants.SEC_SIZE;
  }

  public static int[] ltoi(ArrayList<Integer> list) {
    int size = list.size();
    int[] i = new int[size];
    for(int n = 0; n < size; n++) {
      i[n] = list.get(n).intValue();
    }
    return i;
  }

  public static float[] ltof(ArrayList<Float> list) {
    int size = list.size();
    float[] f = new float[size];
    for(int i = 0; i < size; i++) {
      f[i] = list.get(i).floatValue();
    }
    return f;
  }

  public static ArrayList<Float> atoAL(float[] f) {
    ArrayList<Float> result = new ArrayList<Float>();
    for(int i = 0; i < f.length; i++) {
      result.add(f[i]);
    }
    return result;
  }

  public static ArrayList<Integer> atoAL(int[] n) {
    ArrayList<Integer> result = new ArrayList<Integer>();
    for(int i = 0; i < n.length; i++) {
      result.add(n[i]);
    }
    return result;
  }

  public static String string(String... args) {
    StringBuilder b = new StringBuilder();
    for(String s : args) {b.append(s);}
    return b.toString();
  }

  public static float[] stof(String[] s) {
    float[] f = new float[s.length];
    for(int i = 0; i < f.length; i++) f[i] = Float.parseFloat(s[i]);
    return f;
  }

  public static int[] stoi(String[] s) {
    int[] a = new int[s.length];
    for(int i = 0; i < a.length; i++) a[i] = Integer.parseInt(s[i]);
    return a;
  }

  public static int idxof(int v, int[] a) {
    for(int i = 0; i < a.length; i++) {if(v == a[i]) return i;}
    return -1;
  }

  public static int idxof(float v, float[] a) {
    for(int i = 0; i < a.length; i++) {if(v == a[i]) return i;}
    return -1;
  }

  public static int deg(int a) {
    if(a < 0)
    return Utils.wrap(a % 360 + 360, 0, 360);
    return Utils.wrap(a % 360, 0, 360);
  }
}