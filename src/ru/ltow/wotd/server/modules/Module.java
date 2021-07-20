package ru.ltow.wotd;

class Module {
  protected Entity actor;

  public void setActor(Entity a) {actor = a;}
  public Entity actor() {return actor;}
}