package ru.ltow.wotd;

public class Enums {
  //insert into states (value) values ('standing'),('walking'),('airborne'),('attacking');
  //insert into facings (value) values ('southwest'),('northwest');
  public static enum state {
    STANDING(1),
    WALKING(2),
    AIRBORNE(3),
    ATTACKING(4);

    private final int id;
    private state(int i) {id = i;}
    public int id() {return id;}
  }

  public static enum facing {
    SW(1,225),
    SE(2,315),
    NE(3,45),
    NW(4,135),
    N(5,90),
    W(6,180),
    S(7,270);

    private final int id, deg;
    private facing(int i, int d) {id = i; deg = d;}
    public int id() {return id;}
    public int deg() {return deg;}
  }
}