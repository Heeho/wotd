package ru.ltow.wotd;

public class EntityData {
  private int[] location;

  public EntityData() {
    location = new int[]{0,0};
  }

  public EntityData(int[] l) {
    location = l;
  }

  public int[] location() {return location.clone();}
}