package ru.ltow.wotd;

public class RenderedData {
  private float[] coords;
  private float size;
  private int id, model, tex, state, angle, speed;

  public RenderedData(int sz, int[] c, int i, int m, int t, int s, int a, int sp) {
    size = (float) sz;
    coords = Utils.itof(c);

    id = i;
    model = m;
    tex = t;
    state = s;
    angle = a;
    speed = sp;
  }

  public int id() {return id;}
  public int model() {return model;}
  public int tex() {return tex;}
  public int state() {return state;}
  public int angle() {return angle;}
  public int speed() {return speed;}
  public float size() {return size;}
  public float[] coords() {return coords;}

  public static RenderedData landscape(int[] coords) {
    return new RenderedData(
      1,
      coords,
      ClientConstants.NOID,
      ClientConstants.LANDSCAPE,
      ClientConstants.LANDSCAPE,
      ClientConstants.LANDSCAPE,
      ClientConstants.LANDSCAPE,
      ClientConstants.LANDSCAPE
    );
  }
}