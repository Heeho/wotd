package ru.ltow.wotd;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.AssetManager;

import android.database.sqlite.SQLiteDatabase;

public class App extends Application {
  private static Application a;

  @Override
  public void onCreate() {
    super.onCreate();
    a = this;
  }

  public static SQLiteDatabase db() {
    return a.openOrCreateDatabase("wotd", Context.MODE_PRIVATE, null);
  }

  public static Resources resources() {return a.getResources();}
  public static AssetManager assets() {return a.getAssets();}
}