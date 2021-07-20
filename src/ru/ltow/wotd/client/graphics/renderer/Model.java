package ru.ltow.wotd;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;

import java.util.ArrayList;

public class Model {
  private boolean sharp;
  private IntBuffer indexB;
  private FloatBuffer vertexB;
  private FloatBuffer texcoordsB;
  private FloatBuffer normalB;

  float luminosity, kambient = 1f, kdiffuse = 1f, kspecular = 1f, shininess = 4f;

  private boolean opaque = true, billboard;

  public Model(float[] vertices, int[] indices, float[] normals, float luminosity, int billboard) {
    setVertexB(vertices);
    setIndexB(indices);
    setNormalB(normals);
    setLuminosity(luminosity);
    if(billboard > 0) setBillboard();
  }

  private Model(float[] vertices, int[] indices, float[] texcoords) {
    setVertexB(vertices);
    setIndexB(indices);
    setTexcoordsB(texcoords);
    setNormalB();
  }

  public IntBuffer indexB() {return indexB;}
  public FloatBuffer vertexB() {return vertexB;}
  public FloatBuffer texcoordsB() {return texcoordsB;}
  public FloatBuffer normalB() {return normalB;}

  public float luminosity() {return luminosity;}
  public boolean isOpaque() {return opaque;}
  public boolean isBillboard() {return billboard;}

  public void setLuminosity(float l) {luminosity = (l < 0) ? 0 : (l > 1f) ? 1f : l;}
  public void setBillboard() {billboard = true;}
  public void setIndexB(int[] indices) {indexB = GLUtil.allocateBuffer(indices);}
  public void setVertexB(float[] vertices) {vertexB = GLUtil.allocateBuffer(vertices);}
  public void setTexcoordsB(float[] texcoords) {texcoordsB = GLUtil.allocateBuffer(texcoords);}

  public void setNormalB(float[] normals) {normalB = GLUtil.allocateBuffer(normals);}
  public void setNormalB() {
    //if(isBillboard()) normalB = GLUtil.allocateBuffer(new float[]{0,0.5f,1f,0,0.5f,1f});
    if(sharp) {
      normalB = facenormals();
    } else {
      normalB = vertexnormals();
    }
  }

  public FloatBuffer vertexnormals() {
    return facenormals();
  }

  public FloatBuffer facenormals() {
    int dims = ClientConstants.DIMS;
    int idx0, idx1, idx2;
    float[] p0, p1, p2;
    ArrayList<Float> facenormals = new ArrayList<>();
    float[] facenormal;

    for(int i = 0; i < indexB.capacity(); i += dims) {
      indexB.position(i);
      idx0 = indexB.get();
      idx1 = indexB.get();
      idx2 = indexB.get();
      vertexB.position(dims*idx0);
      p0 = new float[]{vertexB.get(), vertexB.get(), vertexB.get()};
      vertexB.position(dims*idx1);
      p1 = new float[]{vertexB.get(), vertexB.get(), vertexB.get()};
      vertexB.position(dims*idx2);
      p2 = new float[]{vertexB.get(), vertexB.get(), vertexB.get()};
      facenormal = Utils.vnorm(Utils.vcross(Utils.vsub(p2, p0), Utils.vsub(p1, p0)));
      for(int v = 0; v < GLUtil.V_PER_TRIANGLE; v++) {
        facenormals.addAll(Utils.atoAL(facenormal));
      }
    }

    vertexB.position(0);
    indexB.position(0);

    return GLUtil.allocateBuffer(Utils.ltof(facenormals));
  }

  public static Model landscape() {
    /*nlocs: (WotdConstants.ADJACENT_LOCATIONS*2 + 1) ^ 2 -- 9 for 3x3
    nvertices: nlocs*6 (vertices per location: 2 triangles) -- 54 for 3x3
    indices: (0 1 2 3 4 5.. n*6)

    xy always the same and depend on WotdConstants.LOCS_ADJACENT and LOC_SIZE:
    !size is set in rendered by scaling state matrix!
      (-locs_adj*loc_size - loc_size/2, -locs_adj*loc_size - loc_size/2)(
      ..locs_adj(+1, +1)..
      (locs_adj*loc_size - loc_size/2, locs_adj*loc_size - loc_size/2)
    z and terrain are refreshed on server update*/
    int ls = WotdConstants.LOC_SIZE;
    int la = WotdConstants.LOCS_ADJACENT;

    /*generate indices and  float[] vertices in 012023 order for each location
    single location:
    0 3
    1 2
    */
    ArrayList<Integer> indices = new ArrayList<>();
    ArrayList<Float> topl, botl, botr, topr,
    vertices = new ArrayList<>(), texcoords = new ArrayList<>();

    float x, y, xadj, yadj;
    int idx = 0;

    for(int dy = -la; dy <= la; dy++) {
      for(int dx = -la; dx <= la; dx++) {
        x = (float) dx*ls;
        y = (float) dy*ls;
        xadj = x+ls;
        yadj = y+ls;

        topl = Utils.atoAL(new float[]{x,y,0});
        botl = Utils.atoAL(new float[]{x,yadj,0});
        botr = Utils.atoAL(new float[]{xadj,yadj,0});
        topr = Utils.atoAL(new float[]{xadj,y,0});

        texcoords.addAll(Utils.atoAL(new float[]{0,0,0,0,0,0, 0,0,0,0,0,0}));

        vertices.addAll(topl); indices.add(idx++);
        vertices.addAll(botl); indices.add(idx++);
        vertices.addAll(botr); indices.add(idx++);

        vertices.addAll(botr); indices.add(idx++);
        vertices.addAll(topr); indices.add(idx++);
        vertices.addAll(topl); indices.add(idx++);
      }
    }

    //GLUtil.logArr("landscape model vertices: ", Utils.ltof(vertices));

    return new Model(
      Utils.ltof(vertices),
      Utils.ltoi(indices),
      Utils.ltof(texcoords)
    );
  }
}
    /*case CUBE:
    return new Model(
      new float[]{
       -1f, -1f, -2f,
       -1f,  1f, -2f,
        1f,  1f, -2f,
        1f, -1f, -2f,

       -1f, -1f,  0,
       -1f,  1f,  0,
        1f,  1f,  0,
        1f, -1f,  0,
      },
      new int[]{
        0,1,2,2,3,0,
        0,4,5,5,1,0,
        1,5,6,6,2,1,
        2,6,7,7,3,2,
        3,7,4,4,0,3,
        4,7,6,6,5,4
      },
      new float[]{0,1f,1f,1f}
      //new float[]{0,0}
    );*/