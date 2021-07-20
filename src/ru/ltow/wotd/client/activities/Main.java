package ru.ltow.wotd;

import android.os.Bundle;
import android.view.View;
import android.content.Intent;

public class Main extends Base {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
  }

  public void gotoActivity(View v) {
    Class<?> c = getClass();
    switch(v.getId()) {
      case R.id.gameB: c = Game.class; break;
      case R.id.aboutB: c = About.class; break;
      case R.id.dbB: c = DBEditor.class; break;
    }
    startActivity(new Intent(this, c));
  }
}