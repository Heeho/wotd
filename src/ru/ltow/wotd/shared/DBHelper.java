package ru.ltow.wotd;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.HashMap;

public class DBHelper {
  private SQLiteDatabase db; 
  private String[] createtables;

  //private int
  //_id = 0,
  //value = 1;
  private static String
  delimiter = "\\s*;\\s*",
  states = "states",
  facings = "facings",
  //terrains = "terrains",
  entities = "entities";

  public DBHelper() {
    db = App.db();
    createtables = App.resources().getString(R.string.createtables).split(delimiter);
  }

  public void init() {
    for(String s : createtables) db.execSQL(s);
    for(Enums.state s : Enums.state.values()) state(s.name());
    for(Enums.facing f : Enums.facing.values()) facing(f.name());
  }

  public void state(String s) {enuminsert(states, s);}
  public void facing(String s) {enuminsert(facings, s);}

  public void enuminsert(String table, String value) {
    db.execSQL(Utils.string("insert into ",table," (value) values ('",value,"')"));
  }

  public Cursor texs() {
    return db.rawQuery(
      Utils.string(
        "select ",
        "_id,filename,states,facings,frames,framesize ",
        "from texs"
      ), null
    );
  }
  public Cursor models() {
    return db.rawQuery(
      Utils.string(
        "select ",
        "_id,vertices,indices,normals,luminosity,billboard ",
        "from models"
      ), null
    );
  }

  public HashMap<String,Integer> entity(int id) {
    HashMap<String,Integer> h = new HashMap<>();
    Cursor c = db.rawQuery(
      String.format(Utils.string(
      "select ",
      "size,model,tex,",
      Manifestation.class.getSimpleName(),",",
      Target.class.getSimpleName(),",",
      HP.class.getSimpleName(),",",
      Attack.class.getSimpleName(),",",
      StateMachine.class.getSimpleName(),",",
      AI.class.getSimpleName()," ",
      "from ",entities," ",
      "where _id = %d"), id),
    null);

    if(c.getCount() == 0)
    throw new IllegalArgumentException(String.format("no such entity (id %d)", id));

    c.moveToNext();

    for(String column : c.getColumnNames())
    h.put(column, c.getInt(c.getColumnIndex(column)));

    c.close();
    return h;
  }
}