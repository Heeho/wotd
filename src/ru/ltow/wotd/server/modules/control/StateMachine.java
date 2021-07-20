package ru.ltow.wotd;

import java.util.Stack;

public class StateMachine extends Module implements Updatable {
  private Stack<State> stack;
  private final State standing = new Idle();

  public StateMachine() {
    stack = new Stack<>();
    stack(standing);
  }

  public void update() {
    stack.peek().perform();
  }

  public void move(int[] destination) {stack(new Move(destination));}
  public void action() {stack(new Action());}

  //pop state except initial Idle, push next state
  private void stack(State s) {
    clearStack();
    stack.push(s);
  }

  private Stack<State> stack() {return stack;}
  private void clearStack() {while(stack.search(standing) > 1) stack.pop();}

  public Enums.state state() {return stack.peek().state();}

  private class State {
    private Enums.state state;

    private State() {
      state(Enums.state.STANDING);
    }

    public void perform() {}
    public Enums.state state() {return state;}
    public void state(Enums.state s) {state = s;}
  }

  public class Idle extends State {
    public void perform() {
      //nothing
    }
  }

  public class Move extends State {
    private int[] destination;

    private Move(int[] d) {
      destination = d;
      state(Enums.state.WALKING);
    }

    public void perform() {
      /*move to destination then stack.pop() self*/
      if(actor.module(Manifestation.class).move(destination)) {
        //Logger.log("destination reached, ", actor.module(Manifestation.class).coords());
        actor.module(StateMachine.class).stack().pop();
      }
    }
  }

  public class Action extends State {
    private Usable action;
    private Enums.state actionstate;

    private Action() {
      Entity target = actor.module(Target.class).target();
      /*set action based on target stats*/
      if(target.hasModule(HP.class)) {
        action = actor.module(Attack.class);
        //action
        actionstate = Enums.state.ATTACKING;
      } else if(1 == 2) { //target.hasModule(Inventory.class)
        //some other action (pickup, open etc.)
      }
    }

    public void perform() {
      /*if can use action (in range, not on cd etc.), do it, else move in target direction*/
      Entity target = actor.module(Target.class).target();

      if(target == null) {
        clearStack();
        return;
      }

      Manifestation ma = actor.module(Manifestation.class);
      Manifestation mt = target.module(Manifestation.class);

      if(!action.inRangeXY()) {
        stack.push(new Move(Utils.vadd(
          ma.coords(),
          Utils.vndiv(Utils.vdistanceCoords(mt.coords(), ma.coords()), 2)
        )));
        return;
      }

      ma.route(mt.coords());

      if(action.ready()) {
        action.use();
        state(actionstate);
        //set facing
      } else {
        state(Enums.state.STANDING);
      }
    }
  }
}