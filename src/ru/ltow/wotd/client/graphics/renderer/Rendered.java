package ru.ltow.wotd;

import java.util.ArrayList;
import android.opengl.Matrix;

public class Rendered {
  private int id, model;
  private Sprite sprite; //contains tex idx, cartoon rules and counter

  private float[] state, destination;
  private ArrayList<Animation> animations = new ArrayList<>();

  public static Rendered landscape() {
    return new Rendered(RenderedData.landscape(new int[]{0,0,0}));
  }

  public Rendered(RenderedData rd) {
    float[] coords = rd.coords();
    float size = rd.size();
    id = rd.id();

    model = rd.model();
    sprite = new Sprite(rd.tex(), rd.state(), rd.angle(), rd.speed());

    state = new float[16];

    Matrix.setIdentityM(state, 0);
    Matrix.translateM(state, 0, coords[Utils.X], coords[Utils.Y], coords[Utils.Z]);
    Matrix.scaleM(state, 0, size, size, size);

    destination = state.clone();
  }

  public void id(int i) {id = i;}
  public void model(int m) {model = m;}
  public void state(float[] s) {state = s;}
  public void state(ArrayList<Float> s) {state = Utils.ltof(s);}

  public void apply(Rendered r) {
    id(r.id());
    model(r.model());
    sprite.apply(r.sprite());
  }

  public void noid() {id(ClientConstants.NOID);}

  public int id() {return id;}
  public int model() {return model;}
  public Sprite sprite() {return sprite;}
  public float[] state() {return state;}
  public float[] destination() {return destination;}
  public ArrayList<Float> stateAL() {return Utils.atoAL(state);}

  public void addAnimation(Animation a) {animations.add(a);}
  public void setAnimation(Animation a) {animations.clear(); animations.add(a);}
  public void remAnimation(Animation a) {animations.remove(a);}
  public void clearAnimations() {animations.clear();}
  public void performAnimation() {
    for(Animation a : animations) {
      if(a.finished()) continue;
      a.perform();
    }
  }

  public void interpolate(Rendered r, float fps) {
    float correction = 2f;
    float[]
    interpolated = GLUtil.translation(state),
    next = GLUtil.translation(r.state());

    float steps = WotdConstants.TICK_RATIO * fps + correction;
    float[] v = Utils.vndiv(
      Utils.vsub(next, interpolated),
      steps
    );

    setAnimation(new Translation((int) steps, v));
  }

  public void extrapolate(Rendered r, float fps) {
    float correction = 2f;
    float[] 
    extrapolated = GLUtil.translation(state),
    last = GLUtil.translation(destination),
    next = GLUtil.translation(r.state());

    float steps = WotdConstants.TICK_RATIO * fps + correction;
    float[] v = Utils.vndiv(
      Utils.vadd(
        Utils.vsub(next, last),
        Utils.vsub(next, extrapolated)
      ),
      steps
    );

    setAnimation(new Translation((int) steps, v));
    destination = r.state().clone();
  }

  public class Animation {
    /*class for geometric animations (scale, rotation etc)
    maybe should be a builder factory (makeSpinning, makePopup, ..)*/
    public int counter;
    public boolean finished;
    //add fields if needed

    public void perform() {}
    public boolean finished() {return finished;}
  }

  public class Translation extends Animation {
    private int steps;
    private float[] increment;

    public Translation(int s, float[] v) {
      steps = s;
      increment = v;
    }

    public void perform() {
      if(counter++ >= steps) {finished = true; return;}
      state[12] += increment[Utils.X];
      state[13] += increment[Utils.Y];
      state[14] += increment[Utils.Z];
    }
  }
}