package ru.ltow.wotd;

import java.util.Arrays;
import android.util.Log;

public class Logger {
  //all the things loggin
  public static final String TAG = "wotd";

  private static void log(StackTraceElement caller, String... args) {
    StringBuilder b = new StringBuilder();
    b.append(caller).append(" -- ");
    b.append(Utils.string(args));
    Log.e(TAG, b.toString());
  }

  public static void log(String s) {log(callerName(), s);}
  public static void log(String note, float[] a) {log(callerName(), note, Arrays.toString(a));}
  public static void log(String note, int[] a) {log(callerName(), note, Arrays.toString(a));}

  private static StackTraceElement callerName() {
    return Thread.currentThread().getStackTrace()[4];

    /*StringBuilder b = new StringBuilder();
    for(StackTraceElement e : Thread.currentThread().getStackTrace()) {
      b.append(e);
    }
    return b.toString();*/
  }
}