package ru.ltow.wotd;

public class Sprite {
  private float counter, delay, fps = 15f, delaycounter;
  private int tex, state, angle, frame, frames;
  private boolean looped;
  //+linked list node reference for chain animations

  public Sprite(int t, int s, int a, int d) {
    tex = t;
    state = s;
    looped = (s != Enums.state.ATTACKING.id());
    angle = a;
    delay = (float) d;
  }

  public void apply(Sprite s) {
    tex(s.tex());
    angle(s.angle());

    if(s.state() == state || (!looped && frame < frames)) return;

    delay(s.delay());
    state(s.state());
  }

  public void firstframe() {frame = 0;}

  public void tex(int t) {tex = t;}
  public void state(int s) {
    frame = 0;
    counter = 0;
    delaycounter = 0;
    state = s;
    looped = (state != Enums.state.ATTACKING.id());
  }

  public int frames() {return frames;}
  public void frames(int f) {frames = f;}

  public int frame() {return frame;}
  public int frame(float fps) {
    //reset frame for looped sprite if delay reached
    if(++delaycounter > fps*delay) {
      delaycounter = 0;
      if(!looped) frame = 0;
    }
    //advance frame on counter
    if(++counter > fps/this.fps) {
      counter = 0;
      return frame++;
    }
    return frame;
  }

  public void angle(int a) {angle = a;}
  public void delay(float d) {delay = d;}

  public int tex() {return tex;}
  public int state() {return state;}
  public int angle() {return angle;}
  public float delay() {return delay;}
  public boolean looped() {return looped;}

  public int facing(int anglez) {
    int N = Enums.facing.N.deg();
    int W = Enums.facing.W.deg();
    int S = Enums.facing.S.deg();

    int a = Utils.deg(angle - Utils.deg(anglez));

    if(a >= 0 && a < N) {return Enums.facing.NE.id();
    } else if (a >= N && a < W) {return Enums.facing.NW.id();
    } else if (a >= W && a < S) {return Enums.facing.SW.id();
    } else {return Enums.facing.SE.id();}
  }
}