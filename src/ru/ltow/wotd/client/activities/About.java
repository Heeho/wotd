package ru.ltow.wotd;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class About extends Base {
  TextView about;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.about);
    about = findViewById(R.id.aboutTV);
    about.setText(getText(R.string.about));
  }

  public void close(View v) {
    finish();
  }
}