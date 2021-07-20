package ru.ltow.wotd;

public class HP extends Module implements Updatable {
  private int max;
  private int current;
  private int regen;

  public HP() {
    max = 100;
    current = max;
    regen = max / max;
  }

  public void takeDamage(int damage) {
    current -= damage;
  }

  public int current() {
    return current;
  }

  public int max() {
    return max;
  }

  public boolean dead() {return false;}// current <= 0;}

  public void update() {
    //regenerate
  }
}