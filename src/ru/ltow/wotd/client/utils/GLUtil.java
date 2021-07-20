package ru.ltow.wotd;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.ArrayList;

import android.opengl.GLES30;
import android.opengl.Matrix;

import android.graphics.Color;

public class GLUtil {
  public enum genmode { VAO, VBO, FBO, RBO, TEX }

  public static final int DIMS = ClientConstants.DIMS;
  public static final int BYTES_PER_INT32 = 4;
  public static final int BYTES_PER_SHORT = 2;
  public static final int BYTES_PER_FLOAT = 4;
  public static final int COORDS_PER_VERTEX = 3;
  public static final int COORDS_PER_TEXCOORD = 2;
  public static final int MATRIX_LENGTH = 16;
  public static final int MATRIX_ROW_LENGTH = 4;
  public static final int V_PER_TRIANGLE = 3;
  public static final int V_PER_QUAD = 4;

  public static float[] translation(float[] m) {
    return Arrays.copyOfRange(m, MATRIX_LENGTH - MATRIX_ROW_LENGTH, MATRIX_LENGTH - 1);
  }

  public static int linkProgram(String vertexShaderCode, String fragmentShaderCode) {
    int prog = GLES30.glCreateProgram();

    IntBuffer linked = GLUtil.allocateBuffer(new int[1]);

    GLES30.glAttachShader(prog, GLUtil.loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode));
    GLES30.glAttachShader(prog, GLUtil.loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode));
    GLES30.glLinkProgram(prog);

    GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, linked);

    if(linked.get(0) == 0) {
      Logger.log(String.format("program link error: %d",prog));
      GLES30.glGetProgramInfoLog(prog);
      GLES30.glDeleteProgram(prog);
      prog = 0;
    }

    return prog;
  }

  private static int loadShader(int type, String shaderCode) {
    int shader = GLES30.glCreateShader(type);

    IntBuffer compiled = GLUtil.allocateBuffer(new int[1]);

    GLES30.glShaderSource(shader, shaderCode);
    GLES30.glCompileShader(shader);
    GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled);

    if(compiled.get(0) == 0) {
      Logger.log(String.format("shader compile error: %d",type));
      Logger.log(GLES30.glGetShaderInfoLog(shader));
      GLES30.glDeleteShader(shader);
      shader = 0;
    }

    return shader;
  }

  public static int gen(genmode m) {
    IntBuffer name = GLUtil.allocateBuffer(new int[1]);
    switch(m) {
      case VAO: GLES30.glGenVertexArrays(1, name); break;
      case VBO: GLES30.glGenBuffers(1, name); break;
      case FBO: GLES30.glGenFramebuffers(1, name); break;
      case RBO: GLES30.glGenRenderbuffers(1, name); break;
      case TEX: GLES30.glGenTextures(1, name); break;
      default: return 0;
    }
    return name.get(0);
  }

  public static void checkFramebuffer() {
    if(GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) != GLES30.GL_FRAMEBUFFER_COMPLETE)
    Logger.log(String.format("framebuffer incomplete: %d",GLES30.GL_FRAMEBUFFER));
  }

  public static void checkMax() {
    IntBuffer paramHandle = GLUtil.allocateBuffer(new int[1]);
    GLES30.glGetIntegerv(GLES30.GL_MAX_VERTEX_ATTRIBS, paramHandle);

    Logger.log(String.format("current limits: GL_MAX_VERTEX_ATTRIBS: %d",paramHandle.get(0)));
  }

  public static void texNearest(int tex) {
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex);
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
  }

  public static void texLinear(int tex) {
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex);
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
  }

  public static void texClampToEdge(int tex) {
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex);
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
  }

  public static void checkDrawBuffers() {
    String[] attachments = new String[3];

    ArrayList<IntBuffer> drawbuffers = new ArrayList<IntBuffer>();
    drawbuffers.add(GLUtil.allocateBuffer(new int[1]));
    drawbuffers.add(GLUtil.allocateBuffer(new int[1]));
    drawbuffers.add(GLUtil.allocateBuffer(new int[1]));

    GLES30.glGetIntegerv(GLES30.GL_DRAW_BUFFER0, drawbuffers.get(0));
    GLES30.glGetIntegerv(GLES30.GL_DRAW_BUFFER1, drawbuffers.get(1));
    GLES30.glGetIntegerv(GLES30.GL_DRAW_BUFFER2, drawbuffers.get(2));

    for(int i = 0; i < attachments.length; i++) {
    switch(drawbuffers.get(i).get(0)) {
      case GLES30.GL_NONE: attachments[i] = "none"; break;
      case GLES30.GL_BACK: attachments[i] = "back"; break;
      case GLES30.GL_FRONT: attachments[i] = "front"; break;
      case GLES30.GL_COLOR_ATTACHMENT0: attachments[i] = "att0"; break;
      case GLES30.GL_COLOR_ATTACHMENT1: attachments[i] = "att1"; break;
      case GLES30.GL_COLOR_ATTACHMENT2: attachments[i] = "att2"; break;
      default: attachments[i] = "unknown"; break;
    }
    }

    Logger.log(
      String.format("drawbuffer targets: %s: %d, %s: %d, %s: %d",
        attachments[0], drawbuffers.get(0).get(0),
        attachments[1], drawbuffers.get(1).get(0),
        attachments[2], drawbuffers.get(2).get(0)
      )
    );
  }

  public static void checkCurrentFBO() {
    IntBuffer framebufferHandles = GLUtil.allocateBuffer(new int[1]);
    GLES30.glGetIntegerv(GLES30.GL_DRAW_FRAMEBUFFER_BINDING, framebufferHandles);
    Logger.log(String.format("currently bound framebuffer: %d",framebufferHandles.get(0)));
  }

  public static void checkError(String description) {
    String error = null;
    switch(GLES30.glGetError()) {
      case GLES30.GL_NO_ERROR: return;
      case GLES30.GL_INVALID_ENUM: error = "GL_INVALID_ENUM: An unacceptable value is specified for an enumerated argument. The offending command is ignored and has no other side effect than to set the error flag."; break;
      case GLES30.GL_INVALID_VALUE: error = "GL_INVALID_VALUE: A numeric argument is out of range. The offending command is ignored and has no other side effect than to set the error flag."; break;
      case GLES30.GL_INVALID_OPERATION: error = "GL_INVALID_OPERATION: The specified operation is not allowed in the current state. The offending command is ignored and has no other side effect than to set the error flag."; break;
      case GLES30.GL_INVALID_FRAMEBUFFER_OPERATION: error = "GL_INVALID_FRAMEBUFFER_OPERATION: The framebuffer object is not complete. The offending command is ignored and has no other side effect than to set the error flag."; break;
      case GLES30.GL_OUT_OF_MEMORY: error = "GL_OUT_OF_MEMORY: There is not enough memory left to execute the command. The state of the GL is undefined, except for the state of the error flags, after this error is recorded."; break;
      default: error = "non-enum error occured"; break;
    }

    Logger.log(description+": "+error);
  }

  public static IntBuffer allocateBuffer(int[] i) {
    IntBuffer ib = null;
    ByteBuffer bb = ByteBuffer.allocateDirect(i.length * BYTES_PER_INT32);
    bb.order(ByteOrder.nativeOrder());
    ib = bb.asIntBuffer();
    ib.put(i);
    ib.position(0);
    return ib;
  }

  public static FloatBuffer allocateBuffer(float[] f) {
    FloatBuffer fb = null;
    ByteBuffer bb = ByteBuffer.allocateDirect(f.length * BYTES_PER_FLOAT);
    bb.order(ByteOrder.nativeOrder());
    fb = bb.asFloatBuffer();
    fb.put(f);
    fb.position(0);
    return fb;
  }

  public static ShortBuffer allocateBuffer(short[] s) {
    ShortBuffer sb = null;
    ByteBuffer bb = ByteBuffer.allocateDirect(s.length * BYTES_PER_SHORT);
    bb.order(ByteOrder.nativeOrder());
    sb = bb.asShortBuffer();
    sb.put(s);
    sb.position(0);
    return sb;
  }

  public static ByteBuffer allocateBuffer(byte[] b) {
    ByteBuffer bb = ByteBuffer.allocateDirect(b.length);
    bb.order(ByteOrder.nativeOrder());
    bb.put(b);
    bb.position(0);
    return bb;
  }

  public static float[] multiplyMV(float[] m, float[] v) {
    float[] in = new float[]{0,0,0,1f};
    float[] out = new float[MATRIX_ROW_LENGTH];

    for(int i = 0; i < DIMS; i++) in[i] = v[i];

    Matrix.multiplyMV(out, 0, m, 0, in, 0);

    return Arrays.copyOfRange(out, 0, DIMS);
  }

  public static byte[] itorgba(int[] pixelsi) {
    int idx = 0;
    byte[] pixelsb = new byte[pixelsi.length * GLUtil.BYTES_PER_INT32];
    for(int i = 0; i < pixelsi.length; i++) {
      idx = 0;
      pixelsb[i*BYTES_PER_INT32+idx++] = (byte) Color.red(pixelsi[i]);
      pixelsb[i*BYTES_PER_INT32+idx++] = (byte) Color.green(pixelsi[i]);
      pixelsb[i*BYTES_PER_INT32+idx++] = (byte) Color.blue(pixelsi[i]);
      pixelsb[i*BYTES_PER_INT32+idx] = (byte) Color.alpha(pixelsi[i]);
    }
    return pixelsb;
  }

  public static class Triangle {
    private float[] p0, p1, p2, n, prismn1, prismn2, prismn3;

    public Triangle(float[] vertices) {
      if(vertices.length != DIMS*3)
      throw new IllegalArgumentException("wrong vertex data for 3d triangle, must be 9 values");

      p0 = Arrays.copyOfRange(vertices, 0, DIMS);
      p1 = Arrays.copyOfRange(vertices, DIMS, DIMS*2);
      p2 = Arrays.copyOfRange(vertices, DIMS*2, DIMS*3);

      n = Utils.vnorm(Utils.vcross(Utils.vsub(p2, p0), Utils.vsub(p1, p0)));

      prismn1 = Utils.vcross(p0, p1);
      prismn2 = Utils.vcross(p1, p2);
      prismn3 = Utils.vcross(p2, p0);
    }

    public float[] point0() {return p0;}
    public float[] point1() {return p1;}
    public float[] point2() {return p2;}
    public float[] normal() {return n;}

    public boolean intersectedBy(float[] v) {
      /*Logger.log(String.format("dot(v,prismn1) %f",Utils.vdot(v, prismn1)));
      Logger.log(String.format("dot(v,prismn2) %f",Utils.vdot(v, prismn2)));
      Logger.log(String.format("dot(v,prismn3) %f",Utils.vdot(v, prismn3)));*/
      return (
        (Utils.vdot(v, prismn1) <= 0) &&
        (Utils.vdot(v, prismn2) <= 0) &&
        (Utils.vdot(v, prismn3) <= 0)
      );
    }
  }
}