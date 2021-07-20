package ru.ltow.wotd;

public class AI extends Module implements Updatable {
  private long counter, limit;
  private double seed = 600.0;

  public AI() {init();}

  private void init() {
    counter = 0;
    limit = WotdConstants.SECOND * (2L + (long) (1.0 - 3.0 * Math.random()));
  }

  public void update() {
    if(counter <= limit) {
      counter += WotdConstants.TICK;
      return;
    }

    logic();
    command();

    init();
  }

  //override it
  private void logic() {
    carnivore();
  }

  //target 1st entity in los that is lesser by size
  private void carnivore() {
    for(Entity e : actor.module(Target.class).los()) {
      if(e.module(Manifestation.class).size() < actor.module(Manifestation.class).size()) {
        actor.module(Target.class).target(e);
        break;
      }
    }
  }

  //approach and act, how to do disengage?
  private void command() {
    Manifestation m = actor.module(Manifestation.class);

    if(actor.module(Target.class).target() != null) {
      actor.module(StateMachine.class).action();
    } else {
      m.jump();
      actor.module(StateMachine.class).move(Utils.vadd(
        m.coords(),
        new int[]{(int)(seed*(Math.random()-0.5)),(int)(seed*(Math.random()-0.5)),m.coords()[Utils.Z]}
      ));
    }
  }
}