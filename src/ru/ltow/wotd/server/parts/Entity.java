package ru.ltow.wotd;

import java.util.HashMap;

public class Entity {
  private int id;
  protected HashMap<Class<? extends Module>, Module> modules = new HashMap<>();
  private LandscapeData landscapeData;
  private static String size = "size", model = "model", tex = "tex";
  
  public Entity(int id, int[] coords) {
    if(id == 0) {
      addModule(Manifestation.class, new Manifestation(0, coords, 0, 0));
    } else {
      DBHelper dbh = new DBHelper();
      HashMap<String,Integer> data = dbh.entity(id);

      if(data.get(Manifestation.class.getSimpleName()) > 0)
      addModule(Manifestation.class, new Manifestation(
        data.get(size),
        (coords == null) ? new int[]{0,0,0} : coords, //randomize in spawn location
        data.get(model),
        data.get(tex)
      ));
      if(data.get(Target.class.getSimpleName()) > 0) addModule(Target.class, new Target());
      if(data.get(HP.class.getSimpleName()) > 0) addModule(HP.class, new HP());
      if(data.get(Attack.class.getSimpleName()) > 0) addModule(Attack.class, new Attack());
      if(data.get(StateMachine.class.getSimpleName()) > 0)
      addModule(StateMachine.class, new StateMachine());
      if(data.get(AI.class.getSimpleName()) > 0) addModule(AI.class, new AI());
    }

    for(Module m : modules.values()) m.setActor(this);
  }

  public void update() {
    for(Module m : modules.values()) {
      if(m instanceof Updatable) ((Updatable) m).update();
    }
  }

  public int id() {return id;}
  public void id(int i) {id = i;}

  public EntityData entityData() {
    return new EntityData(
      (hasModule(Manifestation.class)) ?
      (module(Manifestation.class).location().clone()) : null
    );
  }

  public RenderedData renderedData(Entity witness) {
    Manifestation m = module(Manifestation.class);

    Enums.state state =
    (m.state().id() == Enums.state.STANDING.id()) ? module(StateMachine.class).state() : m.state();

    int delay = 0;
    switch(state) {
      case ATTACKING: delay = module(Attack.class).delay(); break;
      default: break;
    }

    return new RenderedData(
      m.size(),
      Utils.vdistanceCoords(m.coords(), witness.module(Manifestation.class).coords()),
      id,
      m.model(),
      m.tex(),
      state.id(),
      m.facing(),
      delay
    );
  }

  public void setLandscapeData(LandscapeData ld) {landscapeData = ld;}

  public LandscapeData landscapeData() {
    landscapeData.setRenderedData(RenderedData.landscape(
      Utils.vnmul(module(Manifestation.class).localCoords(), -1)
    ));
    return landscapeData;
  }

  public <T extends Module> T module(Class<T> type) {
    return (T) modules.get(type);
    /*if no module, return null object?
    T module = (T) modules.get(type);
    if(module == null) {return new T();}*/
  }

  public boolean dead() {
    //and/or
    if(hasModule(HP.class) && module(HP.class).dead()) return true;
    return false;
  }

  public <T extends Module> boolean hasModule(Class<T> type) {return modules.containsKey(type);}
  public <T extends Module> void addModule(Class<T> type, T module) {modules.put(type, module);}
}