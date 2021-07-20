package ru.ltow.wotd;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;

public class DBEditor extends Base {
  private EditText query;
  private TextView result;
  private SQLiteDatabase db;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dbeditor);

    query = findViewById(R.id.sqliteET);
    result = findViewById(R.id.sqliteTV);

    db = App.db();
  }

  public void query(View v) {
    Cursor c = db.rawQuery(
      (((CharSequence) query.getText()).length() == 0) ?
        "select * from sqlite_master" : query.getText().toString(),
      null
    );

    String bar = " | ";
    StringBuilder b = (new StringBuilder()).append(bar);

    for(int i = 0; i < c.getColumnCount(); i++) b.append(c.getColumnName(i)).append(bar);

    if(c.getCount() > 0) {
      while(!c.isLast()) {
        c.moveToNext();
        b.append("\n| ");
        for(int i = 0; i < c.getColumnCount(); i++) b.append(c.getString(i)).append(bar);
      }
    }

    result.setText((CharSequence) b.toString());
    c.close();
  }
}