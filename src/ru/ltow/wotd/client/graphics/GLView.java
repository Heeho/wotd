package ru.ltow.wotd;

import android.opengl.GLSurfaceView;
import android.content.Context;

import android.view.MotionEvent;
import android.view.GestureDetector;
import android.util.AttributeSet;

public class GLView extends GLSurfaceView {
  private Networker networker;

  private GLRenderer renderer;
  private static final int EGL_CCV = 3;

  private GLView.Tapper tapper;

  public GLView(Context c) {super(c); ctor(c);}
  public GLView(Context c, AttributeSet a) {super(c, a); ctor(c);}
  //public GLView(Context c, AttributeSet a, int dsa) {super(c, a, dsa); ctor(c);}

  private void ctor(Context c) {
    setEGLContextClientVersion(EGL_CCV);

    tapper = new Tapper(c);
    renderer = new GLRenderer();
    networker = new Networker();

    setRenderer(renderer);
    setRenderMode(RENDERMODE_CONTINUOUSLY);
  }

  public GLRenderer renderer() {return renderer;}
  public Networker networker() {return networker;}

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    return tapper.onTouchEvent(e);
  }

  private class Tapper {
    private GestureDetector tap;
    private float currentX, currentY;
    private int currentFingerSpacing2;

    private Tapper(Context c) {
      tap = new GestureDetector(c, new Tap());
    }

    private int fingerSpacing2(float x0, float y0, float x1, float y1) {
      int[] pointer0 = new int[]{(int) x0, (int) y0};
      int[] pointer1 = new int[]{(int) x1, (int) y1};
      return Utils.vlen2(Utils.vsub(pointer0, pointer1));
    }

    private boolean onTouchEvent(MotionEvent e) {
      int pointerCount = e.getPointerCount();
      float x = e.getX();
      float y = e.getY();

      if(tap.onTouchEvent(e)) {
        queueEvent(new Pick(x, y));
        return true;
      }

      if(pointerCount == 1) {
        if(e.getAction() == MotionEvent.ACTION_MOVE)
        queueEvent(new Drag(x - currentX, y - currentY));
      }

      if(pointerCount == 2) {
        if((e.getAction() == MotionEvent.ACTION_POINTER_DOWN)) {
          currentFingerSpacing2 = fingerSpacing2(e.getX(0),e.getY(0),e.getX(1),e.getY(1));
        } else if(e.getAction() == MotionEvent.ACTION_MOVE) {
          int spacing2 = fingerSpacing2(e.getX(0),e.getY(0),e.getX(1),e.getY(1));
          queueEvent(new Zoom(spacing2 - currentFingerSpacing2));
          currentFingerSpacing2 = spacing2;
        }
      }

      currentX = x;
      currentY = y;
      return true;
    }

    private class Tap extends GestureDetector.SimpleOnGestureListener {
      @Override
      public boolean onDoubleTap(MotionEvent e) {
        networker.send(Command.class, new Command(Command.name.JUMP));
        return false;
      }

      @Override
      public boolean onSingleTapUp(MotionEvent e) {
        return true;
      }
    }

    private class Pick implements Runnable {
      private final float x, y;

      private Pick(float a, float b) {
        x = a;
        y = b;
      }

      @Override
      public void run() {
        int[] destination = renderer.landscapeIntersection(x, y);
        //Logger.log("destination ", destination);

        Command c = null;
        int id = renderer.pickObject(x, y);

        c = (id == ClientConstants.NOID) ? 
          new Command(Command.name.MOVE, destination) :
          new Command(Command.name.ACTION, id);

        networker.send(Command.class, c);
      }
    }

    private class Drag implements Runnable {
      private final float x, y;

      private Drag(float a, float b) {
        x = a;
        y = b;
      }

      @Override
      public void run() {
        renderer.rotateCamera(x, y);
      }
    }

    private class Zoom implements Runnable {
      private final boolean in;

      private Zoom(int fingerSpacingChange2) {
        in = (fingerSpacingChange2 > 0);
      }
      @Override
      public void run() {
        renderer.camera().zoom(in);
      }
    }
  }
}