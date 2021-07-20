package ru.ltow.wotd;

import android.opengl.Matrix;

public class Camera {
  private final float ANGLE_MIN = 15f, ANGLE_MAX = 45f;
  private float angleZ = 45f, angleX = 30f;

  private final float distanceMin = 500f;
  private final float distanceMax = ClientConstants.CAM_DISTANCE;

  private float distance = distanceMax;

  private final float[] center = new float[]{0, 0, 0};
  private final float[] position = new float[]{0, distance, 0};
  private final float[] up = new float[] {0, 0, ClientConstants.UP};

  private final float ROTATE_SPEED = 0.2f;
  private final float ZOOM_FACTOR = 0.95f;

  private final float[] projectionMatrix = new float[GLUtil.MATRIX_LENGTH];
  private final float[] viewMatrix = new float[GLUtil.MATRIX_LENGTH];
  private final float[] vpMatrix = new float[GLUtil.MATRIX_LENGTH];
  private final float[] rotx = new float[GLUtil.MATRIX_LENGTH];
  private final float[] rotz = new float[GLUtil.MATRIX_LENGTH];

  public Camera() {setVM();}

  public float angleX() {return angleX;}
  public float angleZ() {return angleZ;}
  public float distance() {return position[Utils.Y];}

  public void zoom(boolean in) {
    float temp = (in) ? position[Utils.Y]*ZOOM_FACTOR : position[Utils.Y]/ZOOM_FACTOR;

    position[Utils.Y] =
      (temp < distanceMin) ? distanceMin : 
      (temp > distanceMax) ? distanceMax : temp;

    setVM();
  }

  public void rotate(float dx, float dy) {
    float temp = angleX + dy * ROTATE_SPEED;

    angleX =
      (temp < ANGLE_MIN) ? ANGLE_MIN : 
      (temp > ANGLE_MAX) ? ANGLE_MAX : temp;
    angleZ += dx * ROTATE_SPEED;

    setVPM();
  }

  public float[] vpm() {return vpMatrix;}
  public float[] bbxm() {return rotx;}
  public float[] bbzm() {return rotz;}

  public void setPM(float ratio) {
    Matrix.frustumM(
      projectionMatrix, 0, -ratio, ratio, -1f, 1f, ClientConstants.ZNEAR, ClientConstants.ZFAR);
    setVM();
  }

  public void setVM() {
    Matrix.setLookAtM(
      viewMatrix, 0,
      position[Utils.X], position[Utils.Y], position[Utils.Z],
      center[Utils.X], center[Utils.Y], center[Utils.Z],
      up[Utils.X], up[Utils.Y], up[Utils.Z]
    );
    setVPM();
  }

  private void setVPM() {
    Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    Matrix.rotateM(vpMatrix, 0, angleX, 1f, 0, 0);
    Matrix.rotateM(vpMatrix, 0, angleZ, 0, 0, 1f);

    Matrix.setRotateM(rotx, 0, -angleX, 1f, 0, 0);
    Matrix.setRotateM(rotz, 0, -angleZ, 0, 0, 1f);
  }
}