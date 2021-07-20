package ru.ltow.wotd;

public class Physics {
  private int[] g;

  public Physics() {
    g = Utils.vnmul(new int[]{0,0,(int) -ClientConstants.UP}, (int) WotdConstants.G_ACCELERATION);
    //Logger.log("g ", g);
  }

  public void gravitate(Entity e) {
    Manifestation m = e.module(Manifestation.class);
    m.velocity(Utils.vadd(m.velocity(), g));
    m.coords(Utils.vadd(m.coords(), m.velocity()));
  }
}