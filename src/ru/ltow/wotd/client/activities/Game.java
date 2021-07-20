package ru.ltow.wotd;

import android.os.Bundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;

public class Game extends Base {
  private GLView gl;
  private Client client;
  private Server server;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Logger.log("create");

    setContentView(R.layout.game);
    gl = findViewById(R.id.glV); 

    server = new Server();
    client = new Client();
  }

  @Override
  protected void onStart() {
    super.onStart();
    //Logger.log("start");
    server.start();
    client.start();
  }


  @Override
  protected void onStop() {
    //Logger.log("stop");
    client.stop();
    server.stop();
    super.onStop();
  }

  /*@Override protected void onResume() {super.onResume();Logger.log("resume");}
  @Override protected void onRestart() {super.onRestart();Logger.log("restart");}
  @Override protected void onPause() {Logger.log("pause");super.onPause();}
  @Override protected void onDestroy() {Logger.log("destroy");super.onDestroy();}*/

  public class RendererUpdate implements Runnable {
    private final ArrayList<Rendered> rendereds;
    private final EntityData player;
    private final LandscapeData landdata;
    private final Rendered landscape;
   
    public RendererUpdate(Update u) {
      player = u.playerData();
      landdata = u.landscapeData();
      landscape = new Rendered(landdata.renderedData());
      rendereds = new ArrayList<>();
      rendereds.add(landscape);
      for(RenderedData rd : u.renderedData()) {
        rendereds.add(new Rendered(rd));
      }
    }

    @Override
    public void run() {gl.renderer().applyUpdate(this);}
    public ArrayList<Rendered> rendereds() {return rendereds;}
    public EntityData player() {return player;}
    public LandscapeData landdata() {return landdata;}
    public Rendered landscape() {return landscape;}
  }

  private class Client {
    private Timer clientloop;

    private void start() {
      clientloop = new Timer();
      clientloop.scheduleAtFixedRate(new TimerTask() {
        @Override public void run() {
          Update u = gl.networker().receive(Update.class);
          if(u == null) return;
          gl.queueEvent(new RendererUpdate(u));
        }
      }, WotdConstants.DELAY, WotdConstants.TICK);
    }

    private void stop() {
      clientloop.cancel();
    }
  }

  private class Server {
    private Wotd game;
    private Timer serverloop;

    private Server() {
      game = new Wotd();
      game.world().addPlayer(new Networker(), new Entity(WotdConstants.PLAYER, new int[]{0,0,0}));
    }

    private void start() {
      serverloop = new Timer();
      serverloop.scheduleAtFixedRate(new TimerTask() {
        @Override public void run() {game.go();}
      }, 0, WotdConstants.TICK);
    }

    private void stop() {
      serverloop.cancel();
    }
  }
}