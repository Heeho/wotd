package ru.ltow.wotd;

import java.util.ArrayList;

public class Manifestation extends Module implements Updatable {
  private final int dims = ClientConstants.DIMS;
  private int size, size2;
  private int[] coords, location, cmin, cmax, velocity;
  private float[] route;
  private ArrayList<int[]> visibleLocations = new ArrayList<>();

  //speed limit is sqrt(int.max), maths are int
  //x m/s = x*100cm/s = x*100 / (1000 / tick) = x*100 * tick / 1000
  private int speed = (int) (600f * WotdConstants.TICK_RATIO);
  private final int[] jumpAcceleration = new int[]{0,0,(int) (-WotdConstants.G_ACCELERATION*3f)};

  private int model, tex;

  public Manifestation(int sz, int[] c, int m, int t) {
    coords = Utils.wrapCoords(c);
    route = Utils.itof(coords);

    location = new int[dims-1];
    cmin = new int[dims-1];
    cmax = new int[dims-1];
    velocity = new int[dims];
    setLocation();

    size = sz;
    size2 = size * size;
    model = m;
    tex = t;
  }

  public void update() {}

  public int size() {return size;}
  public int[] coords() {return coords;}
  public float[] route() {return route;}
  public int speed() {return speed;}

  public int facing() {
    boolean n = (route[Utils.Y] < 0);
    boolean e = (route[Utils.X] > 0);

    if(n && e) {return Enums.facing.NE.deg();
    } else if (!n && e) {return Enums.facing.SE.deg();
    } else if (!n && !e) {return Enums.facing.SW.deg();
    } else {return Enums.facing.NW.deg();}
  }

  public Enums.state state() {
    return ((velocity[Utils.Z] != 0) ? Enums.state.AIRBORNE : Enums.state.STANDING);
  }

  public int[] velocity() {return velocity;}
  public int[] localCoords() {return Utils.vsub(coords, cmin);}

  public int[] location() {return location;}
  public int model() {return model;}
  public int tex() {return tex;}

  public void route(int[] dest) {route = Utils.itof(Utils.vdistanceCoords(dest, coords));}
  public void coords(int[] c) {coords = Utils.wrapCoords(c);}
  public void velocity(int[] v) {velocity = v;}

  //only xy plane is divided to locations
  public void setLocation() {
    int ls = WotdConstants.LOC_SIZE;

    int[] loc = Utils.wrapLocation(Utils.vndiv(coords, ls));
    location[0] = loc[0];
    location[1] = loc[1];

    //GLUtil.logArr("location: ", location);

    cmin = Utils.vnmul(location, ls);
    cmax = Utils.vnadd(cmin, ls);

    setVisibleLocations();
  }

  private void setVisibleLocations() {
    //set adjacent locations list
    int la = WotdConstants.LOCS_ADJACENT;
    int counter = 0;
    visibleLocations = new ArrayList<>();
    for(int dy = -la; dy <= la; dy++) {
      for(int dx = -la; dx <= la; dx++) {
        visibleLocations.add(counter++, Utils.vadd(location, new int[]{dx, dy}));
      }
    }
  }

  public ArrayList<int[]> visibleLocations() {return visibleLocations;}

  public boolean locationChanged() {
    boolean yn = false;
    for(int i = 0; i < (coords.length - 1); i++) {
      yn = yn || (coords[i] < cmin[i] || coords[i] >= cmax[i]);
    }

    return yn;
  }

  public void jump() {
    if((velocity[Utils.Z] == 0)) {
      velocity[Utils.Z] += jumpAcceleration[Utils.Z] - 1;
    }
  }

  public boolean move(int[] destination) {
    route(destination);

    /*Logger.log("location: ",location);
    Logger.log("coords: ",coords);
    Logger.log("route: ",route);
    Logger.log(String.format("route2: %f",Utils.vlen2(route)));*/

    //destination reached
    if(Utils.distanceCoordsXY2(coords, destination) <= size2) {
      coords(destination);
      return true;
    }

    coords(Utils.vadd(
      coords,
      Utils.ftoi(Utils.vnmul(route, ((float) speed) / Utils.vlen(route)))
    ));

    return false;
  }
}