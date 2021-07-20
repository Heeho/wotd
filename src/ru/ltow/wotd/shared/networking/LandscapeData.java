package ru.ltow.wotd;

public class LandscapeData {
  private RenderedData renddata;
  private float[] depths;
  private int[] terrains;

  public LandscapeData(RenderedData rd, float[] d, int[] t) {
    renddata = rd;
    depths = d;
    terrains = t;
  }

  public void setRenderedData(RenderedData rd) {renddata = rd;}

  public float[] depths() {return depths;}
  public int[] terrains() {return terrains;}
  public RenderedData renderedData() {return renddata;}
}