package ru.ltow.wotd;

import java.util.ArrayList;

public class Update {
  private EntityData playerdata;
  private ArrayList<RenderedData> renddata;
  private LandscapeData landdata;

  public Update(EntityData pd, ArrayList<RenderedData> rd, LandscapeData ld) {
    playerdata = pd;
    renddata = rd;
    landdata = ld;
  }

  public EntityData playerData() {return playerdata;}
  public ArrayList<RenderedData> renderedData() {return renddata;}
  public LandscapeData landscapeData() {return landdata;}
}