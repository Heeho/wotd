package ru.ltow.wotd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

  public class World {
    private ArrayList<Entity> entities;
    private ConcurrentHashMap<Networker,Entity> players;
    private HashMap<Long,Location> locations;
    private ArrayList<Location> visibleLocations;
    //private int[] depthmap = new int[Utils.SEC_SIZE * Utils.SEC_SIZE];

    public World() {
      players = new ConcurrentHashMap<>();
      entities = new ArrayList<>();
      locations = new HashMap<>();
      visibleLocations = new ArrayList<>();
      //spawn();
      addEntity(new Entity(WotdConstants.TOAD, new int[]{200,200,0}));
    }

    public ArrayList<Entity> entities() {
      ArrayList<Entity> result = new ArrayList<>();
      for(Entity e : entities) if(e != null) result.add(e);
      return result;
    }

    public void spawn() {
      for(Location l : locations.values()) {
        if(l.hasNoEntities())
        addEntity(new Entity(
          WotdConstants.TOAD,
          Utils.vadd(new int[3], Utils.vnmul(l.location, WotdConstants.LOC_SIZE))
        ));
      }
    }

    public void addPlayer(Networker n, Entity e) {players.put(n, e);addEntity(e);}
    public void remPlayer(Networker n) {entities.remove(players.get(n)); players.remove(n);}
    public Entity player(Networker n) {return players.get(n);}
    public ConcurrentHashMap<Networker,Entity> players() {return players;}

    public void addEntity(Entity e) {
      if(entities.indexOf(e) >= 0) return;

      int i = entities.indexOf(null);
      if(i < 0) {
        entities.add(e);
        e.id(entities.indexOf(e));
      }
      else { 
        entities.set(i, e);
        e.id(i);
      }
      setLocation(e);
    }

    public void remEntity(Entity e) {
      location(e.module(Manifestation.class).location()).remEntity(e);
      entities.set(entities.indexOf(e), null);
    }

    public void shiftLocation(Entity e) {
      Manifestation m = e.module(Manifestation.class);

      if(m.locationChanged()) {
        location(m.location()).remEntity(e);
        setLocation(e);
      }
    }

    public void setLocation(Entity e) {
      Manifestation m = e.module(Manifestation.class);
      m.setLocation();
      location(m.location()).addEntity(e);

      if(!players.contains(e)) return;

      Location loc = null;
      ArrayList<Float> depths = new ArrayList<>();
      ArrayList<Integer> terrains = new ArrayList<>();

      for(int[] l : m.visibleLocations()) {
        loc = location(l);
        addVisibleLocation(loc);
        depths.addAll(Utils.atoAL(Utils.itof(loc.depths())));
        terrains.addAll(loc.terrains());
      }
      e.setLandscapeData(new LandscapeData(null, Utils.ltof(depths), Utils.ltoi(terrains)));
    }

    private void addVisibleLocation(Location l) {visibleLocations.add(l);}

    public void cleanupLocations() {
      ArrayList<Location> delete = new ArrayList<>();
      for(Location loc : locations.values()) {
        if(loc.hasNoEntities() && !visibleLocations.contains(loc)) delete.add(loc);
      }
      for(Location l : delete) locations.remove(l);
      visibleLocations.clear();
    }

    public void fixZ(Entity e) {
      Manifestation m = e.module(Manifestation.class);
      int height = location(m.location()).height(m.localCoords());

      if(height >= 0) return;

      m.coords()[Utils.Z] += height;
      //set velocity depending on collision? put current location to physics collision check
      m.velocity(Utils.vmul(m.velocity(),new int[]{1,1,0}));
    }

    public Location location(int[] i) {
      long idx = Utils.locationIdx(Utils.wrapLocation(i));
      Location l = locations.get(idx);
      if(l == null) {
        l = new Location(i);
        locations.put(idx, l);
      }
      return l;
    }

public class Location {
  private int depth;
  //private int[] p0, p1, p2, p3, p0p2, p0p1, p0p3, n1, n2;
  private int[] diag;
  private final int[] location;
  private final ArrayList<Entity> entities;
  private ArrayList<Integer> terrains = new ArrayList<>(2); // 2 polygon landscape

  public Location(int[] loc) {
    diag = new int[]{1,-1,0};
    entities = new ArrayList<>();
    location = loc.clone();
    //just random depth for now
    depth = (int) (((double) (WotdConstants.LOC_SIZE / 2)) * (Math.random() - 0.5));
    //test terrain
    terrains.add(1); terrains.add(1);
  }

  public int depth() {return depth;}

  public int height(int[] localCoords) {
    int[] n = n(localCoords); 
    return depth - (int) (((float) Utils.vdot(localCoords, n)) / ((float) n[Utils.Z]));
  }

  private int[] p0() {return px(0,0);}
  private int[] p1() {return px(0,1);}
  private int[] p2() {return px(1,1);}
  private int[] p3() {return px(1,0);}
  private int[] p0p1() {return Utils.vsub(p1(), p0());}
  private int[] p0p2() {return Utils.vsub(p2(), p0());}
  private int[] p0p3() {return Utils.vsub(p3(), p0());}
  private int[] n1() {return Utils.vcross(p0p1(), p0p2());}
  private int[] n2() {return Utils.vcross(p0p2(), p0p3());}

  private int[] px(int x, int y) { 
    int ls = WotdConstants.LOC_SIZE;
    return (x == 0 && y == 0) ?
      new int[]{0,0,depth} :
      new int[]{
        (x > 0) ? x*ls-1 : x*ls,
        (y > 0) ? y*ls-1 : y*ls,
        location(Utils.vadd(location, new int[]{x, y})).depth()
      };
  }

  public int[] n(int[] localCoords) {
    return (Utils.vdot(localCoords, diag) < 0) ? n1() : n2();
  }

  public int[] depths() {
    return new int[]{
      p0()[Utils.Z],
      p1()[Utils.Z],
      p2()[Utils.Z],

      p2()[Utils.Z],
      p3()[Utils.Z],
      p0()[Utils.Z]
    };
  }

  public ArrayList<Integer> terrains() {return terrains;}
  public ArrayList<Entity> entities() {return entities;}
  public boolean hasNoEntities() {return entities.isEmpty();}
  public Entity entity(int i) {return entities.get(i);}
  public void addEntity(Entity e) {entities.add(e);}
  public void remEntity(Entity e) {entities.remove(e);}
}
}