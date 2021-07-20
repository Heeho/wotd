package ru.ltow.wotd;

//import java.util.Arrays;

public class Attack extends Ability implements Updatable, Usable {
  private int dmg;
  private int delay = 1; //sec delay per action
  private int counter, max = ((int) (WotdConstants.SECOND / WotdConstants.TICK)) * delay;

  public Attack() {
    dmg = 50;
  }

  public int delay() {return delay;}

  public boolean ready() {
    return (counter > max);
  }

  public boolean inRangeXY() {
    Entity target = actor.module(Target.class).target();
    /*range might depend on other actor modules*/
    if(target == null) return true;
    Manifestation ma = actor.module(Manifestation.class);
    Manifestation mt = target.module(Manifestation.class);

    return 
    Utils.distanceCoordsXY2(ma.coords(), mt.coords())
    < (ma.size() + mt.size()) * (ma.size() + mt.size() / 4);
  }

  public boolean inRange() {
    Entity target = actor.module(Target.class).target();
    /*range might depend on other actor modules*/
    if(target == null) return true;
    Manifestation ma = actor.module(Manifestation.class);
    Manifestation mt = target.module(Manifestation.class);

    return 
    Utils.distanceCoords2(ma.coords(), mt.coords())
    < (ma.size() + mt.size()) * (ma.size() + mt.size() / 4);
  }

  /*dmg, range and other params might be updated depending on other modules*/
  public boolean use() {
    Entity target = actor.module(Target.class).target();
    if(target != null && ready() && inRange()) {
      /*damage might depend on actor/target modules*/
      target.module(HP.class).takeDamage(dmg);
      if(target.dead()) actor.module(Target.class).target(null);
      counter = 0;
      return true;
    }
    return false;
  }

  /*decrement might depend on other actor modules*/
  public void update() {
    counter++;
  }
}