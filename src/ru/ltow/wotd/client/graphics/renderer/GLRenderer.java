package ru.ltow.wotd;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;

import android.opengl.GLSurfaceView;
import android.opengl.GLES30;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;

import android.opengl.Matrix;

public class GLRenderer implements GLSurfaceView.Renderer {
  private enum mode { OPAQUE, OPAQUE_BB, TRANSPARENT, PICK, PICK_BB, QUAD, BLEND };

  private Storage storage;
  private EntityData player;

  private Rendered landscape;
  private HashMap<Integer,Rendered> rendereds;

  private HashMap<Integer,ArrayList<Rendered>>
    modelInstances, instancesOpaque, instancesTransp, instancesOpaqueBB;

  private ColorProgram opaqueP, opaqueBBP, transpP, pickP, pickBBP;
  private QuadProgram blendP;

  private int fbo, rbo, rboP, texO, texT, texA, tex;

  private final IntBuffer NOID = GLUtil.allocateBuffer(new int[]{ClientConstants.NOID});
  private final FloatBuffer BG = GLUtil.allocateBuffer(new float[]{0,1f,1f,1f});
  //private final FloatBuffer DEPTH = GLUtil.allocateBuffer(new float[]{1f});
  private final FloatBuffer ZERO = GLUtil.allocateBuffer(new float[]{0});

  private Camera camera;
  private float ratio;
  private int width, height;

  private long time;
  private float fps;

  @Override
  public void onSurfaceCreated(GL10 unused, EGLConfig config) {
    camera = new Camera();

    storage = new Storage();
    player = new EntityData();

    rendereds = new HashMap<>();
    landscape = Rendered.landscape();
    rendereds.put(landscape.id(), landscape);

    opaqueP = new ColorProgram(mode.OPAQUE);
    GLUtil.checkError("opaque program");
    opaqueBBP = new ColorProgram(mode.OPAQUE_BB);
    GLUtil.checkError("opaqueBB program");
    transpP = new ColorProgram(mode.TRANSPARENT);
    GLUtil.checkError("transparent program");
    pickP = new ColorProgram(mode.PICK);
    GLUtil.checkError("pick program");
    pickBBP = new ColorProgram(mode.PICK_BB);
    GLUtil.checkError("pickbb program");
    blendP = new QuadProgram(mode.BLEND);
    GLUtil.checkError("blend program");

    instancesOpaque = new HashMap<>();
    instancesTransp = new HashMap<>();
    instancesOpaqueBB = new HashMap<>();

    initInstances();
    GLUtil.checkError("gl surface created");
  }

  public Camera camera() {return camera;}
  public Storage storage() {return storage;}
  public int screenWidth() {return width;}
  public int screenHeight() {return height;}
  public float screenRatio() {return ratio;}
  public Rendered landscape() {return landscape;}
  public HashMap<Integer,Rendered> rendereds() {return rendereds;}

  public void rotateCamera(float x, float y) {
    camera.rotate(x, y);
  }

  private void calcFPS() {
    long current = System.nanoTime();
    fps = (float) (1000000000L / (current - time));
    time = current;
  }

  public void applyUpdate(Game.RendererUpdate ru) {
    if(!Arrays.equals(player.location(), ru.player().location())) {
      float[] shift = Utils.itof(
        Utils.vnmul(
          Utils.vdistanceLocation(ru.player().location(), player.location()),
          WotdConstants.LOC_SIZE
        )
      );
      Matrix.translateM(landscape.state(), 0, shift[Utils.X], shift[Utils.Y], 0);
    }
    setPlayer(ru.player());
    updateLandscape(ru.landdata());
    setRendereds(ru.rendereds());
    initInstances();
  }

  public void setPlayer(EntityData ed) {
    player = ed;
  }

  public void setRendereds(ArrayList<Rendered> input) {
    Rendered current = null;
    HashMap<Integer,Rendered> rnew = new HashMap<>();
    for(Rendered next : input) {
      current = rendereds.get(next.id());
      if(current != null) {
        current.apply(next);
        current.sprite().frames(storage.tex(current.sprite().tex()).frames());
        current.interpolate(next, fps);
        rnew.put(current.id(), current);
      } else {
        rnew.put(next.id(), next);
      }
    }
    rendereds = rnew;
  }

  public void updateLandscape(LandscapeData ld) {
    /*check landscape version, if same do nothing?*/
    Model ls = storage.landscape();
    FloatBuffer vertices = ls.vertexB();
    float[] depths = ld.depths();
    for(int i = 0; i < depths.length; i++) {
      vertices.put(i*ClientConstants.DIMS + Utils.Z, depths[i]);
    }
    ls.setNormalB();
    ls.setTexcoordsB(storage.tex(ld.renderedData().tex()).frames(ld.terrains()));
  }

  public int[] landscapeIntersection(float x, float y) {
    int dims = GLUtil.DIMS;

    FloatBuffer vb = storage.landscape().vertexB().duplicate();

    float ax = camera.angleX();
    float az = camera.angleZ();
    float dist = camera.distance();

    float[] state = landscape.state();

    float[] rotx = new float[GLUtil.MATRIX_LENGTH];
    float[] rotz = new float[GLUtil.MATRIX_LENGTH];

    Matrix.setRotateM(rotx, 0, ax-90f, 1f, 0, 0);
    Matrix.setRotateM(rotz, 0, az, 0, 0, 1f);

    float[] center = new float[]{0, 0, dist};
    float[] translation = GLUtil.translation(state);

    float[] vertices = new float[vb.capacity()];
    float[] vertexin = new float[dims];
    float[] vertex = new float[dims];

    vb.position(0);
    for(int i = 0; i < vb.capacity(); i += dims) {
      vb.get(vertexin, 0, dims);
      vertex = Utils.vadd(
        GLUtil.multiplyMV(rotx, GLUtil.multiplyMV(rotz, Utils.vadd(vertexin, translation))),
        center
      );
      for(int axe = 0; axe < dims; axe++) vertices[i+axe] = vertex[axe];
    }
    vb.position(0);

    float[] v = new float[]{
      (2 * x / ((float) width) - 1f) * ((float) ratio),
      2 * y / ((float) height) - 1f,
      ClientConstants.ZNEAR
    };
    float[] vn = Utils.vnorm(v);

    float cosnv = 0;
    float result = Float.MAX_VALUE;
    float a = 0;
    GLUtil.Triangle triangle = null;

    for(int i = 0; i < vertices.length; i += dims*3) {
      triangle = new GLUtil.Triangle(Arrays.copyOfRange(vertices, i, i+dims*3));
      cosnv = Utils.vdot(triangle.normal(), vn);

      if(cosnv <= 0 || !triangle.intersectedBy(v)) continue;

      float divident = Utils.vdot(triangle.point0(), triangle.normal());
      float divisor = cosnv;

      a = divident / divisor;

      if(a < result) {
        //Logger.log(String.format("cosnv %f, a %f", cosnv, a));
        result = a;
      }
    }

    if(result == Float.MAX_VALUE) return new int[dims];

    float[] raw = Utils.vnmul(vn, result);
    float[] diff = Utils.vsub(raw, center);

    Matrix.setRotateM(rotx, 0, 90f-ax, 1f, 0, 0);
    Matrix.setRotateM(rotz, 0, -az, 0, 0, 1f);

    float[] out = GLUtil.multiplyMV(rotz, GLUtil.multiplyMV(rotx, diff));

    /*Logger.log("raw ", raw);
    Logger.log("diff ", diff);
    Logger.log(String.format("vlen diff %f", Utils.vlen(diff)));
    Logger.log("out ", out);*/

    return Utils.ftoi(out);
  }

  public int pickObject(float screenX, float screenY) {
    int texX = (int) screenX;
    int texY = height - (int) screenY;

    IntBuffer i = GLUtil.allocateBuffer(new int[1]);

    GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, fbo);
    GLES30.glReadBuffer(GLES30.GL_COLOR_ATTACHMENT3);
    GLES30.glReadPixels(texX, texY, 1, 1, GLES30.GL_RED_INTEGER, GLES30.GL_INT, i);

    //Logger.log(String.format("glrenderer object picked at [%d, %d]: %d",texX, texY, i.get(0)));

    GLES30.glReadBuffer(0);

    return i.get(0);
  }

  public void initInstances() {
    /*allow Rendered have multiple models/sprites?*/
    int model = 0;

    instancesOpaque.clear();
    instancesTransp.clear();
    instancesOpaqueBB.clear();

    for(Rendered rendered : rendereds.values()) {
      model = rendered.model();

      if(model > 0) {
        modelInstances = (!storage.model(model).isOpaque()) ? instancesTransp :
          (storage.model(model).isBillboard()) ? instancesOpaqueBB : instancesOpaque;

        if(!modelInstances.containsKey(model))
        modelInstances.put(model, new ArrayList<Rendered>());

        modelInstances.get(model).add(rendered);
      }
    }
  }

  @Override
  public void onDrawFrame(GL10 unused) {
    GLUtil.checkError("render start");
    calcFPS();

    //update model & billboard animation, interpolate data, set sprites
    for(Rendered r : rendereds.values()) r.performAnimation();

    clearBuffers();

    renderOpaque();
    /*renderTransparent();
    renderBlend();
    GLES30.glFinish();*/

    blit();

    renderPick();

    GLUtil.checkError("render end");
  }

  private void clearBuffers() {
    GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, fbo);
    GLES30.glDrawBuffers(4, GLUtil.allocateBuffer(new int[]{
      GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_COLOR_ATTACHMENT1,
      GLES30.GL_COLOR_ATTACHMENT2, GLES30.GL_COLOR_ATTACHMENT3}));
    GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT);
    GLES30.glClearBufferfv(GLES30.GL_COLOR, 0, BG);
    GLES30.glClearBufferfv(GLES30.GL_COLOR, 1, BG);
    GLES30.glClearBufferfv(GLES30.GL_COLOR, 2, ZERO);
    GLES30.glClearBufferiv(GLES30.GL_COLOR, 3, NOID);

    GLUtil.checkError("buffers cleared");
  }

  private void renderOpaque() {
    GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, fbo);
    GLES30.glDrawBuffers(1, GLUtil.allocateBuffer(new int[]{GLES30.GL_COLOR_ATTACHMENT0}));
    GLUtil.checkError("drawbuffers in opaque");

    GLES30.glEnable(GLES30.GL_CULL_FACE);
    GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    GLES30.glDepthFunc(GLES30.GL_LESS);
    GLES30.glDepthMask(true);
    GLES30.glDisable(GLES30.GL_BLEND);

    for(int i : instancesOpaque.keySet())
      opaqueP.render(storage.model(i), instancesOpaque.get(i));
    for(int i : instancesOpaqueBB.keySet())
      opaqueBBP.render(storage.model(i), instancesOpaqueBB.get(i));
    GLUtil.checkError("opaque done");
  }

  private void renderTransparent() {
    GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, fbo);
    GLES30.glDrawBuffers(3, GLUtil.allocateBuffer(new int[]{
      GLES30.GL_NONE, GLES30.GL_COLOR_ATTACHMENT1, GLES30.GL_COLOR_ATTACHMENT2}));
    GLUtil.checkError("drawbuffers in oit");

    GLES30.glDisable(GLES30.GL_CULL_FACE);
    GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    GLES30.glDepthFunc(GLES30.GL_LESS);
    GLES30.glDepthMask(false);
    GLES30.glEnable(GLES30.GL_BLEND);
    GLES30.glBlendFuncSeparate(
      GLES30.GL_ONE, GLES30.GL_ONE,
      GLES30.GL_ZERO, GLES30.GL_ONE_MINUS_SRC_COLOR);

    for(int i : instancesTransp.keySet()) {
      transpP.render(storage.model(i), instancesTransp.get(i));
    }
    GLUtil.checkError("oit done");
  }

  private void renderBlend() {
    GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, fbo);
    GLES30.glDrawBuffers(1, GLUtil.allocateBuffer(new int[]{GLES30.GL_COLOR_ATTACHMENT0}));

    GLUtil.checkError("drawbuffers in blend");

    GLES30.glDisable(GLES30.GL_CULL_FACE);
    GLES30.glDisable(GLES30.GL_DEPTH_TEST);
    GLES30.glEnable(GLES30.GL_BLEND);
    GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

    blendP.render(new int[]{texT, texA});
    GLUtil.checkError("blend done");
  }

  private void renderPick() {
    GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, fbo);
    GLES30.glDrawBuffers(4, GLUtil.allocateBuffer(new int[]{
      GLES30.GL_NONE, GLES30.GL_NONE, GLES30.GL_NONE, GLES30.GL_COLOR_ATTACHMENT3}));
    GLUtil.checkError("drawbuffers in pick");

    GLES30.glEnable(GLES30.GL_CULL_FACE);
    GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    GLES30.glDepthFunc(GLES30.GL_LESS);
    GLES30.glDepthMask(true);
    GLES30.glDisable(GLES30.GL_BLEND);

    GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT);

    for(int i : instancesOpaque.keySet()) {
      pickP.render(storage.model(i), instancesOpaque.get(i));
    }
    for(int i : instancesOpaqueBB.keySet()) {
      pickBBP.render(storage.model(i), instancesOpaqueBB.get(i));
    }
    for(int i : instancesTransp.keySet()) {
      pickP.render(storage.model(i), instancesTransp.get(i));
    }
    GLUtil.checkError("depth done");
  }

  private void blit() {
    GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, fbo);
    GLES30.glReadBuffer(GLES30.GL_COLOR_ATTACHMENT0);

    GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, 0);

    GLES30.glBlitFramebuffer(
      0, 0, width, height, 0, 0, width, height, GLES30.GL_COLOR_BUFFER_BIT, GLES30.GL_LINEAR);
  }

  @Override
  public void onSurfaceChanged(GL10 unused, int w, int h) {
    width = w;
    height = h;

    GLES30.glViewport(0, 0, width, height);

    ratio = ((float) width) / ((float) height);
    camera.setPM(ratio);

    fbo = GLUtil.gen(GLUtil.genmode.FBO);
    rbo = GLUtil.gen(GLUtil.genmode.RBO);
    rboP = GLUtil.gen(GLUtil.genmode.RBO);
    texO = GLUtil.gen(GLUtil.genmode.TEX);
    texT = GLUtil.gen(GLUtil.genmode.TEX);
    texA = GLUtil.gen(GLUtil.genmode.TEX);

    tex = GLUtil.gen(GLUtil.genmode.TEX);

    GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, fbo);

    GLUtil.texNearest(tex);
    GLUtil.texLinear(texO);
    GLUtil.texLinear(texT);
    GLUtil.texLinear(texA);
    GLUtil.texClampToEdge(tex);
    GLUtil.texClampToEdge(texO);
    GLUtil.texClampToEdge(texT);
    GLUtil.texClampToEdge(texA);

    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex);
    GLES30.glTexImage2D(
      GLES30.GL_TEXTURE_2D,
      0,
      GLES30.GL_RGBA8,
      Tex.SIZE,
      Tex.SIZE,
      0,
      GLES30.GL_RGBA,
      GLES30.GL_UNSIGNED_BYTE, 
      GLUtil.allocateBuffer(GLUtil.itorgba(storage.texs().pixels()))
    );
    GLUtil.checkError("surface changed: load texture");

    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texO);
    GLES30.glTexImage2D(
      GLES30.GL_TEXTURE_2D,
      0,
      GLES30.GL_RGBA8,
      width,
      height,
      0,
      GLES30.GL_RGBA,
      GLES30.GL_UNSIGNED_BYTE,
      null);
    GLES30.glFramebufferTexture2D(
      GLES30.GL_DRAW_FRAMEBUFFER,
      GLES30.GL_COLOR_ATTACHMENT0,
      GLES30.GL_TEXTURE_2D,
      texO,
      0);

    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texT);
    GLES30.glTexImage2D(
      GLES30.GL_TEXTURE_2D,
      0,
      GLES30.GL_RGBA8,
      width,
      height,
      0,
      GLES30.GL_RGBA,
      GLES30.GL_UNSIGNED_BYTE,
      null);
    GLES30.glFramebufferTexture2D(
      GLES30.GL_DRAW_FRAMEBUFFER,
      GLES30.GL_COLOR_ATTACHMENT1,
      GLES30.GL_TEXTURE_2D,
      texT,
      0);

    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texA);
    GLES30.glTexImage2D(
      GLES30.GL_TEXTURE_2D,
      0,
      GLES30.GL_R8,
      width,
      height,
      0,
      GLES30.GL_RED,
      GLES30.GL_UNSIGNED_BYTE,
      null);
    GLES30.glFramebufferTexture2D(
      GLES30.GL_DRAW_FRAMEBUFFER,
      GLES30.GL_COLOR_ATTACHMENT2,
      GLES30.GL_TEXTURE_2D,
      texA,
      0);

    GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, rboP);
    GLES30.glRenderbufferStorage(
      GLES30.GL_RENDERBUFFER,
      GLES30.GL_R32I,
      width,
      height);
    GLES30.glFramebufferRenderbuffer(
      GLES30.GL_DRAW_FRAMEBUFFER,
      GLES30.GL_COLOR_ATTACHMENT3,
      GLES30.GL_RENDERBUFFER,
      rboP);

    GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, rbo);
    GLES30.glRenderbufferStorage(
      GLES30.GL_RENDERBUFFER,
      GLES30.GL_DEPTH_COMPONENT24,
      width,
      height);
    GLES30.glFramebufferRenderbuffer(
      GLES30.GL_DRAW_FRAMEBUFFER,
      GLES30.GL_DEPTH_ATTACHMENT,
      GLES30.GL_RENDERBUFFER,
      rbo);

    GLUtil.checkFramebuffer();
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, 0);
    GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, 0);
    GLUtil.checkError("surface changed");
  }

  public class ColorProgram {
    private mode pMode;
    private boolean pick;

    private int 
    programL,
    positionL, normalL, stateL,
    texcoordsL, texcoordsxL, texcoordsyL,
    luminosityL, dirL,
    vpMatrixL, bbxMatrixL, bbzMatrixL,
    idL;

    private int vao, indexBL, stateBL, vertexBL, texcoordsBL, normalBL, idBL;

    private IntBuffer idB;
    private FloatBuffer stateB, texcoordsB,
    dirB = GLUtil.allocateBuffer(Utils.vnorm(new float[]{0,0.5f,1f}));

    ArrayList<Float> statesAL, texcoordsAL;
    ArrayList<Integer> idsAL;

    private ColorProgram(mode m) {
      pMode = m;
      pick = (pMode == mode.PICK || pMode == mode.PICK_BB);

      String vs = Utils.string(
        "#version 300 es\n",
        "precision highp float;",
        "precision highp int;",

        "uniform mat4 vp;",

        "in mat4 state;",
        "in vec4 position;",

        "in vec3 normal_;",
        "out vec3 normal;"
      );
      String fs = Utils.string(
        "#version 300 es\n",
        "precision highp float;",
        "precision highp int;",

        "in vec3 normal;",
        "uniform float luminosity;",
        "uniform vec3 dir;",

        "uniform sampler2D tex;",
        "in vec2 texcoords;"
      );

      //vertex shader
      switch(pMode) {
        case OPAQUE_BB: vs = Utils.string(vs,
        "in vec4 texcoordsx_;",
        "in vec4 texcoordsy_;",
        "out vec2 texcoords;",

        "uniform mat4 bbx;",
        "uniform mat4 bbz;",

        "void main() {",
        "  gl_Position = vp * state * bbz * bbx * position;",
        "  normal = mat3(state * bbz) * normal_;",
        "  texcoords = vec2(texcoordsx_[gl_VertexID], texcoordsy_[gl_VertexID]);",
        "}"); break;
        case TRANSPARENT:
        case OPAQUE: vs = Utils.string(vs,  
        "in vec2 texcoords_;",
        "out vec2 texcoords;",

        "void main() {",
        "  gl_Position = vp * state * position;",
        "  normal = mat3(state) * normal_;",
        "  texcoords = texcoords_;",
        "}"); break;
        case PICK_BB:
        case PICK:
        vs = Utils.string(
        "#version 300 es\n",
        "precision highp float;",
        "precision highp int;",

        "uniform mat4 vp;",
        "uniform mat4 bbx;",
        "uniform mat4 bbz;",

        "in mat4 state;",
        "in vec4 position;",

        "in int id_;",
        "flat out int id;",

        "void main() {",
        "  gl_Position = vp * ",
        ((pMode == mode.PICK) ? "state" : "state * bbz * bbx")," * position;",
        "  id = id_;",
        "}"); break;
        default: vs = "not_a_vertex_shader"; break;
      }

      //fragment shader
      switch(pMode) {
        case OPAQUE:
        case OPAQUE_BB: fs = Utils.string(fs,
        "layout (location = 0) out vec4 _fragO;",
        "void main() {",
        "  _fragO = vec4(texture(tex, texcoords));",
        "  if(_fragO.a == 0.0) discard;",

        "  float kdir = dot(normalize(normal), dir);",
        "  if(kdir < 0.0) kdir = 0.0;",
        "  _fragO.rgb *= clamp(luminosity + kdir, 0.0, 1.0);",
        "}"); break;
        case TRANSPARENT: fs = Utils.string(fs,
        "layout (location = 1) out vec4 _fragT;",
        "layout (location = 2) out float _fragA;",
        "void main() {",
        "  float w = clamp(pow(gl_FragCoord.z, 1.5), 1e-2, 3e3);",
        "  vec4 color = texture(tex, texcoords);",
        "  _fragT = vec4(color.rgb * color.a * w, color.a);",
        "  _fragA = color.a * w;",
        "}"); break;
        case PICK_BB:
        case PICK: fs = Utils.string(
        "#version 300 es\n",
        "precision highp float;",
        "precision highp int;",

        "flat in int id;",

        "layout (location = 3) out int _fragP;",
        "void main() {",
        "  _fragP = id;",
        "}"); break;
        default: fs = "not_a_fragment_shader"; break;
      }

      programL = GLUtil.linkProgram(vs, fs);

      vpMatrixL = GLES30.glGetUniformLocation(programL, "vp");
      bbxMatrixL = GLES30.glGetUniformLocation(programL, "bbx");
      bbzMatrixL = GLES30.glGetUniformLocation(programL, "bbz");

      vao = GLUtil.gen(GLUtil.genmode.VAO);
      indexBL = GLUtil.gen(GLUtil.genmode.VBO);
      vertexBL = GLUtil.gen(GLUtil.genmode.VBO);
      stateBL = GLUtil.gen(GLUtil.genmode.VBO);

      if(!pick) {
        luminosityL = GLES30.glGetUniformLocation(programL, "luminosity");
        dirL = GLES30.glGetUniformLocation(programL, "dir");
        texcoordsBL = GLUtil.gen(GLUtil.genmode.VBO);
        normalBL = GLUtil.gen(GLUtil.genmode.VBO);
      } else {
        idBL = GLUtil.gen(GLUtil.genmode.VBO);
      }

    GLES30.glBindVertexArray(vao);

      GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBL);

      if(!pick) {
        normalL = GLES30.glGetAttribLocation(programL, "normal_");
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, normalBL);
        GLES30.glEnableVertexAttribArray(normalL);
        GLES30.glVertexAttribPointer(
          normalL,
          GLUtil.COORDS_PER_VERTEX,
          GLES30.GL_FLOAT,
          false,
          GLUtil.COORDS_PER_VERTEX * GLUtil.BYTES_PER_FLOAT,
          0);

        switch(pMode) {
        case OPAQUE:
        texcoordsL = GLES30.glGetAttribLocation(programL, "texcoords_");
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, texcoordsBL);
        GLES30.glEnableVertexAttribArray(texcoordsL);
          GLES30.glVertexAttribPointer(
            texcoordsL,
            GLUtil.COORDS_PER_TEXCOORD,
            GLES30.GL_FLOAT,
            false,
            GLUtil.COORDS_PER_TEXCOORD * GLUtil.BYTES_PER_FLOAT,
            0);
          break;
        case OPAQUE_BB:
        texcoordsxL = GLES30.glGetAttribLocation(programL, "texcoordsx_");
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, texcoordsBL);
        GLES30.glEnableVertexAttribArray(texcoordsxL);
          GLES30.glVertexAttribPointer(
            texcoordsxL,
            GLUtil.V_PER_QUAD,
            GLES30.GL_FLOAT,
            false,
            GLUtil.COORDS_PER_TEXCOORD * GLUtil.V_PER_QUAD * GLUtil.BYTES_PER_FLOAT,
            0);
          GLES30.glVertexAttribDivisor(texcoordsxL, 1);

        texcoordsyL = GLES30.glGetAttribLocation(programL, "texcoordsy_");
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, texcoordsBL);
        GLES30.glEnableVertexAttribArray(texcoordsyL);
          GLES30.glVertexAttribPointer(
            texcoordsyL,
            GLUtil.V_PER_QUAD,
            GLES30.GL_FLOAT,
            false,
            GLUtil.COORDS_PER_TEXCOORD * GLUtil.V_PER_QUAD * GLUtil.BYTES_PER_FLOAT,
            GLUtil.V_PER_QUAD * GLUtil.BYTES_PER_FLOAT);
          GLES30.glVertexAttribDivisor(texcoordsyL, 1);
          break;
        }
      } else {
        idL = GLES30.glGetAttribLocation(programL, "id_");
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, idBL);
        GLES30.glEnableVertexAttribArray(idL);
        GLES30.glVertexAttribIPointer(
          idL,
          1,
          GLES30.GL_INT,
          GLUtil.BYTES_PER_INT32,
          0);
        GLES30.glVertexAttribDivisor(idL, 1);
      }

      positionL = GLES30.glGetAttribLocation(programL, "position");
      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBL);
      GLES30.glEnableVertexAttribArray(positionL);
      GLES30.glVertexAttribPointer(
        positionL,
        GLUtil.COORDS_PER_VERTEX,
        GLES30.GL_FLOAT,
        false,
        GLUtil.COORDS_PER_VERTEX * GLUtil.BYTES_PER_FLOAT,
        0);

      stateL = GLES30.glGetAttribLocation(programL, "state");
      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, stateBL);
      for (int i = 0; i < GLUtil.MATRIX_LENGTH/GLUtil.MATRIX_ROW_LENGTH; i++) {
        int stateML = stateL + i;
        GLES30.glEnableVertexAttribArray(stateML);
        GLES30.glVertexAttribPointer(
          stateML,
          GLUtil.MATRIX_ROW_LENGTH,
          GLES30.GL_FLOAT,
          false,
          GLUtil.MATRIX_LENGTH * GLUtil.BYTES_PER_FLOAT,
        i * GLUtil.MATRIX_ROW_LENGTH * GLUtil.BYTES_PER_FLOAT);
        GLES30.glVertexAttribDivisor(stateML, 1);
      }

    GLES30.glBindVertexArray(0);

      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
      GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void render(Model model, ArrayList<Rendered> instances) {
      statesAL = new ArrayList<>();
      for(Rendered r : instances) statesAL.addAll(Utils.atoAL(r.state()));
      stateB = GLUtil.allocateBuffer(Utils.ltof(statesAL));

      switch(pMode) {
        case OPAQUE:
          texcoordsB = model.texcoordsB();
          break;
        case OPAQUE_BB:
          texcoordsAL = new ArrayList<>();
          float[] tc = null;
          Sprite s = null;
          for(Rendered r : instances) {
            s = r.sprite();
            tc = storage.texs().get(s.tex()).frame(
              s.state(),
              s.facing((int) camera.angleZ()),
              s.frame(fps),
              s.looped()
            );
            texcoordsAL.add(tc[0]);texcoordsAL.add(tc[2]);
            texcoordsAL.add(tc[4]);texcoordsAL.add(tc[6]);
            texcoordsAL.add(tc[1]);texcoordsAL.add(tc[3]);
            texcoordsAL.add(tc[5]);texcoordsAL.add(tc[7]);
          }
          texcoordsB = GLUtil.allocateBuffer(Utils.ltof(texcoordsAL));
          break;
        case PICK:
        case PICK_BB:
          idsAL = new ArrayList<>();
          for(Rendered r : instances) idsAL.add(Integer.valueOf(r.id()));
          idB = GLUtil.allocateBuffer(Utils.ltoi(idsAL));
          GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, idBL);
          GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            idB.capacity() * GLUtil.BYTES_PER_INT32,
            idB,
            GLES30.GL_STREAM_DRAW);
          break;
      }

      GLES30.glUseProgram(programL);

      if(!pick) {
        GLES30.glUniform1f(luminosityL, model.luminosity());
        GLES30.glUniform3fv(dirL, 1, dirB);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, normalBL);
        GLES30.glBufferData(
          GLES30.GL_ARRAY_BUFFER,
          model.normalB().capacity() * GLUtil.BYTES_PER_FLOAT,
          model.normalB(),
          GLES30.GL_STREAM_DRAW);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, texcoordsBL);
        GLES30.glBufferData(
          GLES30.GL_ARRAY_BUFFER,
          texcoordsB.capacity() * GLUtil.BYTES_PER_FLOAT,
          texcoordsB,
          GLES30.GL_STREAM_DRAW);
      }

      GLES30.glUniformMatrix4fv(vpMatrixL, 1, false, GLUtil.allocateBuffer(camera.vpm()));
      GLES30.glUniformMatrix4fv(bbxMatrixL, 1, false, GLUtil.allocateBuffer(camera.bbxm()));
      GLES30.glUniformMatrix4fv(bbzMatrixL, 1, false, GLUtil.allocateBuffer(camera.bbzm()));

      GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBL);
      GLES30.glBufferData(
        GLES30.GL_ELEMENT_ARRAY_BUFFER,
        model.indexB().capacity() * GLUtil.BYTES_PER_INT32,
        model.indexB(),
        GLES30.GL_STREAM_DRAW);

      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBL);
      GLES30.glBufferData(
        GLES30.GL_ARRAY_BUFFER,
        model.vertexB().capacity() * GLUtil.BYTES_PER_FLOAT,
        model.vertexB(),
        GLES30.GL_STREAM_DRAW);

      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, stateBL);
      GLES30.glBufferData(
        GLES30.GL_ARRAY_BUFFER,
        stateB.capacity() * GLUtil.BYTES_PER_FLOAT,
        stateB,
        GLES30.GL_STREAM_DRAW);

    GLES30.glBindVertexArray(vao);
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex);

      GLES30.glDrawElementsInstanced(
        GLES30.GL_TRIANGLES,
        //GLES30.GL_LINES,
        model.indexB().capacity(),
        GLES30.GL_UNSIGNED_INT,
        0,
        stateB.capacity() / GLUtil.MATRIX_ROW_LENGTH);

    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    GLES30.glBindVertexArray(0);

      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
      GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);
    }
  }

  public class QuadProgram {
    private int[] texL;
    private int programL, posL, texcoordL;
    private int vao, vbo;

    private FloatBuffer quadB;

    private QuadProgram(mode m) {
      String vs = Utils.string(
        "#version 300 es\n",

        "in vec2 position;",

        "in vec2 texcoords_;",
        "out vec2 texcoords;",

        "void main() {",
        "  gl_Position = vec4(position.xy, 0.0, 1.0);",
        "  texcoords = texcoords_;",
        "}");
      String fs = Utils.string(
        "#version 300 es\n",
        "precision mediump float;",

        "in vec2 texcoords;",
        "out vec4 _frag;");

      switch(m) {
        case QUAD: fs += Utils.string(
        "layout (location = 0) uniform sampler2D tex;",

        "void main() {",
        "  _frag = texture(tex, texcoords);",
        "}");
        programL = GLUtil.linkProgram(vs, fs);
        texL = new int[1];
        texL[0] = GLES30.glGetUniformLocation(programL, "tex");
        break;
        case BLEND: fs += Utils.string(
        "layout (location = 0) uniform sampler2D texT;",
        "layout (location = 1) uniform sampler2D texA;",

        "float maxof(vec4 v) {",
        "  return clamp(max(max(v.r, max(v.g, v.b)), v.a), 0.0, 1.0);",
        "}",

        "void main() {",
        "  float eps = 0.00001;",

        "  vec4 color = vec4(texture(texT, texcoords).rgb, texture(texA, texcoords).r);",
        "  if(isinf(maxof(abs(color)))) color.rgb = vec3(color.a);",

        "  float a = texture(texT, texcoords).a;",
        "  if(a == 1.0) discard;",

        "  _frag = vec4(color.rgb / max(color.a, eps), (1.0 - a));",
        "}");
        programL = GLUtil.linkProgram(vs, fs);
        texL = new int[2];
        texL[0] = GLES30.glGetUniformLocation(programL, "texT");
        texL[1] = GLES30.glGetUniformLocation(programL, "texA");
        break;
        default: fs = "not_a_fragment_shader"; return;
      }

      vao = GLUtil.gen(GLUtil.genmode.VAO);
      vbo = GLUtil.gen(GLUtil.genmode.VBO);

      quadB = GLUtil.allocateBuffer(new float[]{
      //vertices  texcoords
       -1f,  1f,   0,  1f,  //top left
       -1f, -1f,   0,   0,  //bottom left
        1f, -1f,  1f,   0,  //bottom right
        1f, -1f,  1f,   0,  //bottom right
        1f,  1f,  1f,  1f   //top right
       -1f,  1f,   0,  1f,  //top left
      });

    GLES30.glBindVertexArray(vao);

      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
      GLES30.glBufferData(
        GLES30.GL_ARRAY_BUFFER,
        quadB.capacity() * GLUtil.BYTES_PER_FLOAT,
        quadB,
        GLES30.GL_STATIC_DRAW);

      posL = GLES30.glGetAttribLocation(programL, "position");
      GLES30.glEnableVertexAttribArray(posL);
      GLES30.glVertexAttribPointer(
        posL,
        2,
        GLES30.GL_FLOAT,
        false,
        4 * GLUtil.BYTES_PER_FLOAT,
        0 * GLUtil.BYTES_PER_FLOAT);

      texcoordL = GLES30.glGetAttribLocation(programL, "texcoords_");
      GLES30.glEnableVertexAttribArray(texcoordL);
      GLES30.glVertexAttribPointer(
        texcoordL,
        2,
        GLES30.GL_FLOAT,
        false,
        4 * GLUtil.BYTES_PER_FLOAT,
        2 * GLUtil.BYTES_PER_FLOAT);

    GLES30.glBindVertexArray(0);

      GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
    }

    private void render(int[] tex) {
      GLES30.glUseProgram(programL);

      for(int i = 0; i < tex.length; i++) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + i);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[i]);
        GLES30.glUniform1i(texL[i], i);
      }

    GLES30.glBindVertexArray(vao);

      GLES30.glDrawArrays(
        GLES30.GL_TRIANGLES,
        0,
        6);

    GLES30.glBindVertexArray(0);

      for(int i = (tex.length - 1); i >= 0; i--) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + i);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
      }
    }
  }
}