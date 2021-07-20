package ru.ltow.wotd;

import java.util.Collection;
import java.util.HashMap;

import android.database.Cursor;

public class Storage {
  private final String delimiter;
  private DBHelper dbh;

  private HashMap<Integer,Model> models;
  private Tex texs;

  public Storage() {
    dbh = new DBHelper();
    delimiter = App.resources().getString(R.string.delimiter);
    models = loadModels();
    texs = loadTexs();
  }

  private HashMap<Integer,Model> loadModels() {
    HashMap<Integer,Model> h = new HashMap<>();

    Cursor c = dbh.models();

    h.put(ClientConstants.LANDSCAPE, Model.landscape());
    c.moveToNext(); //moving past landscape dummy

    while(!c.isLast()) {
      c.moveToNext();
      h.put(
        c.getInt(c.getColumnIndex("_id")),
        new Model(
          Utils.stof(c.getString(c.getColumnIndex("vertices")).split(delimiter)),
          Utils.stoi(c.getString(c.getColumnIndex("indices")).split(delimiter)),
          Utils.stof(c.getString(c.getColumnIndex("normals")).split(delimiter)),
          c.getFloat(c.getColumnIndex("luminosity")),
          c.getInt(c.getColumnIndex("billboard"))
        )
      );
    }

    c.close();
    return h;
  }

  private Tex loadTexs() {
    Tex t = new Tex();

    Cursor c = dbh.texs();

    while(!c.isLast()) {
      c.moveToNext();

      t.insert(
        c.getInt(c.getColumnIndex("_id")),
        c.getString(c.getColumnIndex("filename")),
        Utils.stoi(c.getString(c.getColumnIndex("states")).split(delimiter)),
        Utils.stoi(c.getString(c.getColumnIndex("facings")).split(delimiter)),
        c.getInt(c.getColumnIndex("frames")),
        c.getInt(c.getColumnIndex("framesize"))
      );
    }

    c.close();

    //transparency
    for(int i = 0; i < t.pixels().length; i++) {
      if(t.pixels()[i] == 0xffffffff) t.pixels()[i] = 0;
    }

    return t;
  }

  public Model landscape() {return models.get(ClientConstants.LANDSCAPE);}
  public Model model(int id) {return models.get(id);}
  public Collection<Model> models() {return models.values();}
  public Tex.Node tex(int id) {return texs.get(id);}
  public Tex texs() {return texs;}
}