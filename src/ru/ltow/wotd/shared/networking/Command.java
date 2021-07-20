package ru.ltow.wotd;

import java.util.HashMap;

public class Command {
  public enum name { NONE, MOVE, JUMP, ACTION }

  private HashMap<Class<? extends Object>,Object> items;
  private Command.name n = name.NONE;

  public Command(Command.name n) {
    this.n = n;
    switch(n) {
      case JUMP:
      case NONE: break;
      default: items = new HashMap<>();
    }  
  }

  public Command(Command.name n, int i) {
    this(n);
    switch(n) {
      case ACTION: items.put(Command.Code.class, new Code(i)); break;
    }
  }

  public Command(Command.name n, int[] d) {
    this(n);
    switch(n) {
      case MOVE: items.put(Command.Direction.class, new Direction(d)); break;
    }
  }

  public Command.name name() {return n;}
  public <T> T get(Class<T> type) {return (T) items.get(type);}

  public static class Code {
    private int code = ClientConstants.NOID;
    private Code(int i) {code = i;}
    public int code() {return code;}
  }

  public static class Direction {
    private int[] direction = new int[ClientConstants.DIMS];
    private Direction(int[] d) {direction = d;}
    public int[] direction() {return direction;}
  }
}