package ru.ltow.wotd;

/*
0) distance 1 = [cm]
1) Range is checked for adjacent locations only (3x3), so, for legal integer math, location size must be 0.. sqrt( [2^31] / [3*3 + 3*3] ) which is 10922. Is less in practise (500cm x 3 = 15 sq.meters area, even lesser for detailed landscape).
2) Sector and map sizes are 0.. 2^(31) - 1 (long int for location and sector idx).
Range is never checked on this scale so no power math
3) LOC_SIZE * SEC_SIZE < Integer.MAX_VALUE: set locsize, set secsize = intmax / locsize
4) SEC_SIZE ^ 2 < Long.MAX_VALUE*/
public class WotdConstants {
  public static final long SECOND = 1000;
  public static final long TICK = 60;
  public static final long DELAY = 200;
  public static final float TICK_RATIO = ((float) TICK) / ((float) SECOND);

  //9.81m/s = 981 cm/s / (1000ms/tick) = 981 * tick / 1000
  public static float G_ACCELERATION = 981f * TICK_RATIO;

  //sectors per map
  public static final int MAP_SIZE = 1;
  //lengths per loc side, >100 opt for now. Definetely need to optimize landscape rendering
  //bugs at 2000
  public static final int LOC_SIZE = 1000;
  //locations per sec side, <2^31.5 long.max
  public static final int SEC_SIZE =
    (Integer.MAX_VALUE - (int) (ClientConstants.ZFAR / ClientConstants.ZNEAR) + 1) / LOC_SIZE;
  public static final int SEC_HALF = SEC_SIZE/2;
  //max value for coords, must be < int.max, guaranteed by sec size definition
  public static final int COORDS_MAX = SEC_SIZE * LOC_SIZE;
  public static final int LOCS_ADJACENT = 2 * (int) (ClientConstants.CAM_DISTANCE / (float) LOC_SIZE);

  //temp
  public static final int BEACON = 1;
  public static final int PLAYER = 2;
  public static final int TOAD = 3;
}