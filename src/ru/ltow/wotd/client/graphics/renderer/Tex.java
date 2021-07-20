package ru.ltow.wotd;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;

public class Tex {
  public static final int SIZE = 2048;

  private Node root;
  private final int[] pixels;

  public Tex() {
    pixels = new int[SIZE*SIZE];
    Arrays.fill(pixels, 0xffff00ff);
    root = new Node(0, 0, SIZE, SIZE);
  }

  public Node get(int id) {
    return root.get(id);
  }

  public int[] pixels() {return pixels;}

  public float[] frame(int id, int state, int facing, int frame, boolean looped) {
    return root.get(id).frame(state, facing, frame, looped);
  }

  public void insert(int id, String filename, int[] states, int[] facings,int frames,int framesize) {
    if(!root.insert(new Node(id, filename, states, facings, frames, framesize)))
    throw new IllegalArgumentException("not enough space, increase texture size");
  }

  public class Node {
    private int id, frames, framesize;
    private int[] states, facings;

    private float[] texcoords, zeroframe;
    private String filename;

    private Node[] child;
    private int originx, originy, width, height;
    private float ox, oy, d;

    private Node(int originx, int originy, int width, int height) {
      rectangle(originx, originy, width, height);
    }

    private Node(int i, String fn, int[] s, int[] f, int fr, int fs) {
      id = i;
      filename = fn;
      states = s;
      facings = f;
      frames = fr;
      framesize = fs;
    }

    private Node get(int id) {
      if(this.id == id) return this;
      if(child == null) return null;

      Node n = child[0].get(id);

      n = (n == null) ? child[1].get(id) : n;
      return n;
    }

    private boolean insert(Node n) {
      boolean result = false;

      if(child == null) {
        try {
          Bitmap b = BitmapFactory.decodeStream(App.assets().open(n.filename()), null, null);

          int bw = n.frames() * n.framesize();
          int bh = n.states().length * n.facings().length * n.framesize();

          if(bw > b.getWidth()) bw = b.getWidth();
          if(bh > b.getHeight()) bh = b.getHeight();

          int dw = width - bw, dh = height - bh;

          if(dw < 0 || dh < 0) {}
          else {
            b.getPixels(pixels, originx + originy*SIZE, SIZE, 0, 0, bw, bh);

            if(dh > dw) {
              child = new Node[]{
                new Node(originx + bw, originy, dw, bh),
                new Node(originx, originy + bh, width, dh)
              };
            } else {
              child = new Node[]{
                new Node(originx, originy + bh, bw, dh),
                new Node(originx + bw, originy, dw, height)
              };
            }

            copy(n);
            rectangle(originx, originy, bw, bh);
            texcoords((float) originx, (float) originy, (float) bw, (float) bh);

            result = true;
          }

          b.recycle();
        } catch(IOException e) {e.printStackTrace();}

        return result;

      } else {
        result = (child[0].insert(n)) ? true : child[1].insert(n);
        return result;
      }
    }

    private void rectangle(int ox, int oy, int w, int h) {
      originx = ox;
      originy = oy;
      width = w;
      height = h;
    }

    private void texcoords(float originx, float originy, float width, float height) {
      float s = (float) SIZE;

      texcoords = Utils.vndiv(
        new float[]{
          originx, originy,
          originx, originy + height - 1f,
          originx + width - 1f, originy + height - 1f,
          originx + width - 1f, originy
        },
        s
      );

      ox = texcoords[0];
      oy = texcoords[1];
      //w = texcoords[4] - texcoords[0];
      //h = texcoords[5] - texcoords[1];
      d = (float) framesize / s;

      zeroframe = new float[]{
        ox, oy,
        ox, oy + d,
        ox + d, oy + d,
        ox + d, oy
      };
    }

    public void copy(Node n) {
      id = n.id();
      filename = n.filename();
      states = n.states();
      facings = n.facings();
      frames = n.frames();
      framesize = n.framesize();
    }

    public int id() {return id;}
    public String filename() {return filename;}
    public int[] states() {return states;}
    public int[] facings() {return facings;}
    public int frames() {return frames;}
    public int framesize() {return framesize;}

    public float[] texcoords() {return texcoords;}
    public float[] frame(int frame) {return texcoords;}
    public float[] frame(int facing, int frame) {return texcoords;}

    public float[] frame(int s, int f, int fr, boolean looped) {
      int frame = fr % frames;
      int state = 0;
      int facing = Utils.idxof(f, facings);

      state =
      (fr < frames) ? Utils.idxof(s, states) :
      (looped) ? Utils.idxof(s, states) : Utils.idxof(Enums.state.STANDING.id(), states);

      if(state < 0 || facing < 0) return zeroframe;

      float dx = ((float) frame) * d;
      float dy = ((float) (state * facings.length + facing)) * d;

      return Utils.vadd(
        zeroframe,
        new float[]{dx,dy, dx,dy, dx,dy, dx,dy}
      );
    }

    public float[] frames(int[] frames) {
      ArrayList<Float> framesAL = new ArrayList<>();
      for(int f : frames) framesAL.addAll(Utils.atoAL(Arrays.copyOfRange(frame(1,1,f-1,true),0,6)));
      return Utils.ltof(framesAL);
    }
  }
}