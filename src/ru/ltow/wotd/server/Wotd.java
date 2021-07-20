package ru.ltow.wotd;

import java.util.ArrayList;

public class Wotd {
  private DBHelper db;
  private World world;
  private Physics physics;

  public Wotd() {
    db = new DBHelper();
    db.init();
    world = new World();
    physics = new Physics();
  }

  public void clearPlayers() {
    for(Networker client : world.players().keySet()) {
      world.remPlayer(client);
      client = null;
    }
  }

  public void go() {
    applyCommands();
    updateState();
    sendUpdates();
  }

  private void updateState() {
    for(Entity e : world.entities()) {
      //saving null references to preserve id as AL idx, so checking them for null
        if(e.dead()) {
          world.remEntity(e);
        } else {
          //fill visible entities
          if(e.hasModule(Target.class)) {
            e.module(Target.class).los().clear();
            for(int[] l : e.module(Manifestation.class).visibleLocations()) {
              e.module(Target.class).los().addAll(world.location(l).entities());
            }
          }
          e.update();
          physics.gravitate(e);
          world.shiftLocation(e);
          world.fixZ(e);
        }
    }
    world.cleanupLocations();
  }

  private void sendUpdates() {
    Entity player = null;
    //int[] playerLocation = null;
    ArrayList<RenderedData> rd;

    for(Networker client : world.players().keySet()) {
      player = world.player(client);
      //playerLocation = player.module(Manifestation.class).location();
      rd = new ArrayList<>();
      for(int[] l : player.module(Manifestation.class).visibleLocations()) {
        for(Entity e : world.location(l).entities()) rd.add(e.renderedData(player));
      }

      client.send(Update.class, new Update(
        player.entityData(),
        rd,
        player.landscapeData()
      ));
    }
  }

  private void applyCommands() {
    Command command = null;
    Entity player = null;
    StateMachine sm = null;

    for(Networker client : world.players().keySet()) {
      player = world.player(client);
      command = client.receive(Command.class);
      sm = player.module(StateMachine.class);

      if(command == null) continue;

      switch(command.name()) {
        case MOVE:
          sm.move(Utils.vadd(
            player.module(Manifestation.class).coords(),
            command.get(Command.Direction.class).direction()
          ));
        break;
        case JUMP:
          player.module(Manifestation.class).jump();
        break;
        case ACTION:
          player.module(Target.class).
            target(world.entities().get(command.get(Command.Code.class).code()));
          sm.action();
        break;
        default: break;
      }
    }
  }

  public World world() {return world;}
}