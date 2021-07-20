package ru.ltow.wotd;

import java.util.ArrayList;

public class Target extends Module implements Updatable {
  private ArrayList<Entity> los = new ArrayList<>();
  private Entity target;

  public Target() {
    //
  }

  public ArrayList<Entity> los() {return los;}
  public void target(Entity t) {target = t;}
  public Entity target() {return target;}

  public void update() {
    //
  }
}