class Nxt$Curve25519
{
  public static final int KEY_SIZE = 32;
  public static final byte[] ZERO = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
  public static final byte[] PRIME = { -19, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 127 };
  public static final byte[] ORDER = { -19, -45, -11, 92, 26, 99, 18, 88, -42, -100, -9, -94, -34, -7, -34, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16 };
  private static final int P25 = 33554431;
  private static final int P26 = 67108863;
  
  public static final void clamp(byte[] k)
  {
    k[31] = ((byte)(k[31] & 0x7F));
    k[31] = ((byte)(k[31] | 0x40)); int 
      tmp22_21 = 0;k[tmp22_21] = ((byte)(k[tmp22_21] & 0xF8));
  }
  
  public static final void keygen(byte[] P, byte[] s, byte[] k)
  {
    clamp(k);
    core(P, s, k, null);
  }
  
  public static final void curve(byte[] Z, byte[] k, byte[] P)
  {
    core(Z, null, k, P);
  }
  
  public static final boolean sign(byte[] v, byte[] h, byte[] x, byte[] s)
  {
    byte[] tmp1 = new byte[65];
    byte[] tmp2 = new byte[33];
    for (int i = 0; i < 32; i++) {
      v[i] = 0;
    }
    i = mula_small(v, x, 0, h, 32, -1);
    mula_small(v, v, 0, ORDER, 32, (15 - v[31]) / 16);
    mula32(tmp1, v, s, 32, 1);
    divmod(tmp2, tmp1, 64, ORDER, 32);
    int w = 0;
    for (i = 0; i < 32; i++) {
      w |= (v[i] = tmp1[i]);
    }
    return w != 0;
  }
  
  public static final void verify(byte[] Y, byte[] v, byte[] h, byte[] P)
  {
    byte[] d = new byte[32];
    
    Nxt.Curve25519.long10[] p = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
    Nxt.Curve25519.long10[] s = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
    Nxt.Curve25519.long10[] yx = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
    Nxt.Curve25519.long10[] yz = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
    Nxt.Curve25519.long10[] t1 = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
    Nxt.Curve25519.long10[] t2 = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
    
    int vi = 0;int hi = 0;int di = 0;int nvh = 0;
    


    set(p[0], 9);
    unpack(p[1], P);
    





    x_to_y2(t1[0], t2[0], p[1]);
    sqrt(t1[0], t2[0]);
    int j = is_negative(t1[0]);
    t2[0]._0 += 39420360L;
    mul(t2[1], BASE_2Y, t1[0]);
    sub(t1[j], t2[0], t2[1]);
    add(t1[(1 - j)], t2[0], t2[1]);
    cpy(t2[0], p[1]);
    t2[0]._0 -= 9L;
    sqr(t2[1], t2[0]);
    recip(t2[0], t2[1], 0);
    mul(s[0], t1[0], t2[0]);
    sub(s[0], s[0], p[1]);
    s[0]._0 -= 486671L;
    mul(s[1], t1[1], t2[0]);
    sub(s[1], s[1], p[1]);
    s[1]._0 -= 486671L;
    mul_small(s[0], s[0], 1L);
    mul_small(s[1], s[1], 1L);
    for (int i = 0; i < 32; i++)
    {
      vi = vi >> 8 ^ v[i] & 0xFF ^ (v[i] & 0xFF) << 1;
      hi = hi >> 8 ^ h[i] & 0xFF ^ (h[i] & 0xFF) << 1;
      nvh = vi ^ hi ^ 0xFFFFFFFF;
      di = nvh & (di & 0x80) >> 7 ^ vi;
      di ^= nvh & (di & 0x1) << 1;
      di ^= nvh & (di & 0x2) << 1;
      di ^= nvh & (di & 0x4) << 1;
      di ^= nvh & (di & 0x8) << 1;
      di ^= nvh & (di & 0x10) << 1;
      di ^= nvh & (di & 0x20) << 1;
      di ^= nvh & (di & 0x40) << 1;
      d[i] = ((byte)di);
    }
    di = (nvh & (di & 0x80) << 1 ^ vi) >> 8;
    

    set(yx[0], 1);
    cpy(yx[1], p[di]);
    cpy(yx[2], s[0]);
    set(yz[0], 0);
    set(yz[1], 1);
    set(yz[2], 1);
    






    vi = 0;
    hi = 0;
    for (i = 32; i-- != 0;)
    {
      vi = vi << 8 | v[i] & 0xFF;
      hi = hi << 8 | h[i] & 0xFF;
      di = di << 8 | d[i] & 0xFF;
      for (j = 8; j-- != 0;)
      {
        mont_prep(t1[0], t2[0], yx[0], yz[0]);
        mont_prep(t1[1], t2[1], yx[1], yz[1]);
        mont_prep(t1[2], t2[2], yx[2], yz[2]);
        
        int k = ((vi ^ vi >> 1) >> j & 0x1) + ((hi ^ hi >> 1) >> j & 0x1);
        
        mont_dbl(yx[2], yz[2], t1[k], t2[k], yx[0], yz[0]);
        
        k = di >> j & 0x2 ^ (di >> j & 0x1) << 1;
        mont_add(t1[1], t2[1], t1[k], t2[k], yx[1], yz[1], p[(di >> j & 0x1)]);
        

        mont_add(t1[2], t2[2], t1[0], t2[0], yx[2], yz[2], s[(((vi ^ hi) >> j & 0x2) >> 1)]);
      }
    }
    int k = (vi & 0x1) + (hi & 0x1);
    recip(t1[0], yz[k], 0);
    mul(t1[1], yx[k], t1[0]);
    
    pack(t1[1], Y);
  }
  
  private static final void cpy32(byte[] d, byte[] s)
  {
    for (int i = 0; i < 32; i++) {
      d[i] = s[i];
    }
  }
  
  private static final int mula_small(byte[] p, byte[] q, int m, byte[] x, int n, int z)
  {
    int v = 0;
    for (int i = 0; i < n; i++)
    {
      v += (q[(i + m)] & 0xFF) + z * (x[i] & 0xFF);
      p[(i + m)] = ((byte)v);
      v >>= 8;
    }
    return v;
  }
  
  private static final int mula32(byte[] p, byte[] x, byte[] y, int t, int z)
  {
    int n = 31;
    int w = 0;
    for (int i = 0; i < t; i++)
    {
      int zy = z * (y[i] & 0xFF);
      w += mula_small(p, p, i, x, 31, zy) + (p[(i + 31)] & 0xFF) + zy * (x[31] & 0xFF);
      
      p[(i + 31)] = ((byte)w);
      w >>= 8;
    }
    p[(i + 31)] = ((byte)(w + (p[(i + 31)] & 0xFF)));
    return w >> 8;
  }
  
  private static final void divmod(byte[] q, byte[] r, int n, byte[] d, int t)
  {
    int rn = 0;
    int dt = (d[(t - 1)] & 0xFF) << 8;
    if (t > 1) {
      dt |= d[(t - 2)] & 0xFF;
    }
    while (n-- >= t)
    {
      int z = rn << 16 | (r[n] & 0xFF) << 8;
      if (n > 0) {
        z |= r[(n - 1)] & 0xFF;
      }
      z /= dt;
      rn += mula_small(r, r, n - t + 1, d, t, -z);
      q[(n - t + 1)] = ((byte)(z + rn & 0xFF));
      mula_small(r, r, n - t + 1, d, t, -rn);
      rn = r[n] & 0xFF;
      r[n] = 0;
    }
    r[(t - 1)] = ((byte)rn);
  }
  
  private static final int numsize(byte[] x, int n)
  {
    while ((n-- != 0) && (x[n] == 0)) {}
    return n + 1;
  }
  
  private static final byte[] egcd32(byte[] x, byte[] y, byte[] a, byte[] b)
  {
    int bn = 32;
    for (int i = 0; i < 32; i++)
    {
      int tmp21_20 = 0;y[i] = tmp21_20;x[i] = tmp21_20;
    }
    x[0] = 1;
    int an = numsize(a, 32);
    if (an == 0) {
      return y;
    }
    byte[] temp = new byte[32];
    for (;;)
    {
      int qn = bn - an + 1;
      divmod(temp, b, bn, a, an);
      bn = numsize(b, bn);
      if (bn == 0) {
        return x;
      }
      mula32(y, x, temp, qn, -1);
      
      qn = an - bn + 1;
      divmod(temp, a, an, b, bn);
      an = numsize(a, an);
      if (an == 0) {
        return y;
      }
      mula32(x, y, temp, qn, -1);
    }
  }
  
  private static final void unpack(Nxt.Curve25519.long10 x, byte[] m)
  {
    x._0 = (m[0] & 0xFF | (m[1] & 0xFF) << 8 | (m[2] & 0xFF) << 16 | (m[3] & 0xFF & 0x3) << 24);
    
    x._1 = ((m[3] & 0xFF & 0xFFFFFFFC) >> 2 | (m[4] & 0xFF) << 6 | (m[5] & 0xFF) << 14 | (m[6] & 0xFF & 0x7) << 22);
    
    x._2 = ((m[6] & 0xFF & 0xFFFFFFF8) >> 3 | (m[7] & 0xFF) << 5 | (m[8] & 0xFF) << 13 | (m[9] & 0xFF & 0x1F) << 21);
    
    x._3 = ((m[9] & 0xFF & 0xFFFFFFE0) >> 5 | (m[10] & 0xFF) << 3 | (m[11] & 0xFF) << 11 | (m[12] & 0xFF & 0x3F) << 19);
    
    x._4 = ((m[12] & 0xFF & 0xFFFFFFC0) >> 6 | (m[13] & 0xFF) << 2 | (m[14] & 0xFF) << 10 | (m[15] & 0xFF) << 18);
    
    x._5 = (m[16] & 0xFF | (m[17] & 0xFF) << 8 | (m[18] & 0xFF) << 16 | (m[19] & 0xFF & 0x1) << 24);
    
    x._6 = ((m[19] & 0xFF & 0xFFFFFFFE) >> 1 | (m[20] & 0xFF) << 7 | (m[21] & 0xFF) << 15 | (m[22] & 0xFF & 0x7) << 23);
    
    x._7 = ((m[22] & 0xFF & 0xFFFFFFF8) >> 3 | (m[23] & 0xFF) << 5 | (m[24] & 0xFF) << 13 | (m[25] & 0xFF & 0xF) << 21);
    
    x._8 = ((m[25] & 0xFF & 0xFFFFFFF0) >> 4 | (m[26] & 0xFF) << 4 | (m[27] & 0xFF) << 12 | (m[28] & 0xFF & 0x3F) << 20);
    
    x._9 = ((m[28] & 0xFF & 0xFFFFFFC0) >> 6 | (m[29] & 0xFF) << 2 | (m[30] & 0xFF) << 10 | (m[31] & 0xFF) << 18);
  }
  
  private static final boolean is_overflow(Nxt.Curve25519.long10 x)
  {
    return ((x._0 > 67108844L) && ((x._1 & x._3 & x._5 & x._7 & x._9) == 33554431L) && ((x._2 & x._4 & x._6 & x._8) == 67108863L)) || (x._9 > 33554431L);
  }
  
  private static final void pack(Nxt.Curve25519.long10 x, byte[] m)
  {
    int ld = 0;int ud = 0;
    
    ld = (is_overflow(x) ? 1 : 0) - (x._9 < 0L ? 1 : 0);
    ud = ld * -33554432;
    ld *= 19;
    long t = ld + x._0 + (x._1 << 26);
    m[0] = ((byte)(int)t);
    m[1] = ((byte)(int)(t >> 8));
    m[2] = ((byte)(int)(t >> 16));
    m[3] = ((byte)(int)(t >> 24));
    t = (t >> 32) + (x._2 << 19);
    m[4] = ((byte)(int)t);
    m[5] = ((byte)(int)(t >> 8));
    m[6] = ((byte)(int)(t >> 16));
    m[7] = ((byte)(int)(t >> 24));
    t = (t >> 32) + (x._3 << 13);
    m[8] = ((byte)(int)t);
    m[9] = ((byte)(int)(t >> 8));
    m[10] = ((byte)(int)(t >> 16));
    m[11] = ((byte)(int)(t >> 24));
    t = (t >> 32) + (x._4 << 6);
    m[12] = ((byte)(int)t);
    m[13] = ((byte)(int)(t >> 8));
    m[14] = ((byte)(int)(t >> 16));
    m[15] = ((byte)(int)(t >> 24));
    t = (t >> 32) + x._5 + (x._6 << 25);
    m[16] = ((byte)(int)t);
    m[17] = ((byte)(int)(t >> 8));
    m[18] = ((byte)(int)(t >> 16));
    m[19] = ((byte)(int)(t >> 24));
    t = (t >> 32) + (x._7 << 19);
    m[20] = ((byte)(int)t);
    m[21] = ((byte)(int)(t >> 8));
    m[22] = ((byte)(int)(t >> 16));
    m[23] = ((byte)(int)(t >> 24));
    t = (t >> 32) + (x._8 << 12);
    m[24] = ((byte)(int)t);
    m[25] = ((byte)(int)(t >> 8));
    m[26] = ((byte)(int)(t >> 16));
    m[27] = ((byte)(int)(t >> 24));
    t = (t >> 32) + (x._9 + ud << 6);
    m[28] = ((byte)(int)t);
    m[29] = ((byte)(int)(t >> 8));
    m[30] = ((byte)(int)(t >> 16));
    m[31] = ((byte)(int)(t >> 24));
  }
  
  private static final void cpy(Nxt.Curve25519.long10 out, Nxt.Curve25519.long10 in)
  {
    out._0 = in._0;out._1 = in._1;
    out._2 = in._2;out._3 = in._3;
    out._4 = in._4;out._5 = in._5;
    out._6 = in._6;out._7 = in._7;
    out._8 = in._8;out._9 = in._9;
  }
  
  private static final void set(Nxt.Curve25519.long10 out, int in)
  {
    out._0 = in;out._1 = 0L;
    out._2 = 0L;out._3 = 0L;
    out._4 = 0L;out._5 = 0L;
    out._6 = 0L;out._7 = 0L;
    out._8 = 0L;out._9 = 0L;
  }
  
  private static final void add(Nxt.Curve25519.long10 xy, Nxt.Curve25519.long10 x, Nxt.Curve25519.long10 y)
  {
    x._0 += y._0;x._1 += y._1;
    x._2 += y._2;x._3 += y._3;
    x._4 += y._4;x._5 += y._5;
    x._6 += y._6;x._7 += y._7;
    x._8 += y._8;x._9 += y._9;
  }
  
  private static final void sub(Nxt.Curve25519.long10 xy, Nxt.Curve25519.long10 x, Nxt.Curve25519.long10 y)
  {
    x._0 -= y._0;x._1 -= y._1;
    x._2 -= y._2;x._3 -= y._3;
    x._4 -= y._4;x._5 -= y._5;
    x._6 -= y._6;x._7 -= y._7;
    x._8 -= y._8;x._9 -= y._9;
  }
  
  private static final Nxt.Curve25519.long10 mul_small(Nxt.Curve25519.long10 xy, Nxt.Curve25519.long10 x, long y)
  {
    long t = x._8 * y;
    xy._8 = (t & 0x3FFFFFF);
    t = (t >> 26) + x._9 * y;
    xy._9 = (t & 0x1FFFFFF);
    t = 19L * (t >> 25) + x._0 * y;
    xy._0 = (t & 0x3FFFFFF);
    t = (t >> 26) + x._1 * y;
    xy._1 = (t & 0x1FFFFFF);
    t = (t >> 25) + x._2 * y;
    xy._2 = (t & 0x3FFFFFF);
    t = (t >> 26) + x._3 * y;
    xy._3 = (t & 0x1FFFFFF);
    t = (t >> 25) + x._4 * y;
    xy._4 = (t & 0x3FFFFFF);
    t = (t >> 26) + x._5 * y;
    xy._5 = (t & 0x1FFFFFF);
    t = (t >> 25) + x._6 * y;
    xy._6 = (t & 0x3FFFFFF);
    t = (t >> 26) + x._7 * y;
    xy._7 = (t & 0x1FFFFFF);
    t = (t >> 25) + xy._8;
    xy._8 = (t & 0x3FFFFFF);
    xy._9 += (t >> 26);
    return xy;
  }
  
  private static final Nxt.Curve25519.long10 mul(Nxt.Curve25519.long10 xy, Nxt.Curve25519.long10 x, Nxt.Curve25519.long10 y)
  {
    long x_0 = x._0;long x_1 = x._1;long x_2 = x._2;long x_3 = x._3;long x_4 = x._4;
    long x_5 = x._5;long x_6 = x._6;long x_7 = x._7;long x_8 = x._8;long x_9 = x._9;
    
    long y_0 = y._0;long y_1 = y._1;long y_2 = y._2;long y_3 = y._3;long y_4 = y._4;
    long y_5 = y._5;long y_6 = y._6;long y_7 = y._7;long y_8 = y._8;long y_9 = y._9;
    
    long t = x_0 * y_8 + x_2 * y_6 + x_4 * y_4 + x_6 * y_2 + x_8 * y_0 + 2L * (x_1 * y_7 + x_3 * y_5 + x_5 * y_3 + x_7 * y_1) + 38L * (x_9 * y_9);
    


    xy._8 = (t & 0x3FFFFFF);
    t = (t >> 26) + x_0 * y_9 + x_1 * y_8 + x_2 * y_7 + x_3 * y_6 + x_4 * y_5 + x_5 * y_4 + x_6 * y_3 + x_7 * y_2 + x_8 * y_1 + x_9 * y_0;
    


    xy._9 = (t & 0x1FFFFFF);
    t = x_0 * y_0 + 19L * ((t >> 25) + x_2 * y_8 + x_4 * y_6 + x_6 * y_4 + x_8 * y_2) + 38L * (x_1 * y_9 + x_3 * y_7 + x_5 * y_5 + x_7 * y_3 + x_9 * y_1);
    


    xy._0 = (t & 0x3FFFFFF);
    t = (t >> 26) + x_0 * y_1 + x_1 * y_0 + 19L * (x_2 * y_9 + x_3 * y_8 + x_4 * y_7 + x_5 * y_6 + x_6 * y_5 + x_7 * y_4 + x_8 * y_3 + x_9 * y_2);
    


    xy._1 = (t & 0x1FFFFFF);
    t = (t >> 25) + x_0 * y_2 + x_2 * y_0 + 19L * (x_4 * y_8 + x_6 * y_6 + x_8 * y_4) + 2L * (x_1 * y_1) + 38L * (x_3 * y_9 + x_5 * y_7 + x_7 * y_5 + x_9 * y_3);
    


    xy._2 = (t & 0x3FFFFFF);
    t = (t >> 26) + x_0 * y_3 + x_1 * y_2 + x_2 * y_1 + x_3 * y_0 + 19L * (x_4 * y_9 + x_5 * y_8 + x_6 * y_7 + x_7 * y_6 + x_8 * y_5 + x_9 * y_4);
    


    xy._3 = (t & 0x1FFFFFF);
    t = (t >> 25) + x_0 * y_4 + x_2 * y_2 + x_4 * y_0 + 19L * (x_6 * y_8 + x_8 * y_6) + 2L * (x_1 * y_3 + x_3 * y_1) + 38L * (x_5 * y_9 + x_7 * y_7 + x_9 * y_5);
    


    xy._4 = (t & 0x3FFFFFF);
    t = (t >> 26) + x_0 * y_5 + x_1 * y_4 + x_2 * y_3 + x_3 * y_2 + x_4 * y_1 + x_5 * y_0 + 19L * (x_6 * y_9 + x_7 * y_8 + x_8 * y_7 + x_9 * y_6);
    


    xy._5 = (t & 0x1FFFFFF);
    t = (t >> 25) + x_0 * y_6 + x_2 * y_4 + x_4 * y_2 + x_6 * y_0 + 19L * (x_8 * y_8) + 2L * (x_1 * y_5 + x_3 * y_3 + x_5 * y_1) + 38L * (x_7 * y_9 + x_9 * y_7);
    


    xy._6 = (t & 0x3FFFFFF);
    t = (t >> 26) + x_0 * y_7 + x_1 * y_6 + x_2 * y_5 + x_3 * y_4 + x_4 * y_3 + x_5 * y_2 + x_6 * y_1 + x_7 * y_0 + 19L * (x_8 * y_9 + x_9 * y_8);
    


    xy._7 = (t & 0x1FFFFFF);
    t = (t >> 25) + xy._8;
    xy._8 = (t & 0x3FFFFFF);
    xy._9 += (t >> 26);
    return xy;
  }
  
  private static final Nxt.Curve25519.long10 sqr(Nxt.Curve25519.long10 x2, Nxt.Curve25519.long10 x)
  {
    long x_0 = x._0;long x_1 = x._1;long x_2 = x._2;long x_3 = x._3;long x_4 = x._4;
    long x_5 = x._5;long x_6 = x._6;long x_7 = x._7;long x_8 = x._8;long x_9 = x._9;
    
    long t = x_4 * x_4 + 2L * (x_0 * x_8 + x_2 * x_6) + 38L * (x_9 * x_9) + 4L * (x_1 * x_7 + x_3 * x_5);
    
    x2._8 = (t & 0x3FFFFFF);
    t = (t >> 26) + 2L * (x_0 * x_9 + x_1 * x_8 + x_2 * x_7 + x_3 * x_6 + x_4 * x_5);
    
    x2._9 = (t & 0x1FFFFFF);
    t = 19L * (t >> 25) + x_0 * x_0 + 38L * (x_2 * x_8 + x_4 * x_6 + x_5 * x_5) + 76L * (x_1 * x_9 + x_3 * x_7);
    

    x2._0 = (t & 0x3FFFFFF);
    t = (t >> 26) + 2L * (x_0 * x_1) + 38L * (x_2 * x_9 + x_3 * x_8 + x_4 * x_7 + x_5 * x_6);
    
    x2._1 = (t & 0x1FFFFFF);
    t = (t >> 25) + 19L * (x_6 * x_6) + 2L * (x_0 * x_2 + x_1 * x_1) + 38L * (x_4 * x_8) + 76L * (x_3 * x_9 + x_5 * x_7);
    

    x2._2 = (t & 0x3FFFFFF);
    t = (t >> 26) + 2L * (x_0 * x_3 + x_1 * x_2) + 38L * (x_4 * x_9 + x_5 * x_8 + x_6 * x_7);
    
    x2._3 = (t & 0x1FFFFFF);
    t = (t >> 25) + x_2 * x_2 + 2L * (x_0 * x_4) + 38L * (x_6 * x_8 + x_7 * x_7) + 4L * (x_1 * x_3) + 76L * (x_5 * x_9);
    

    x2._4 = (t & 0x3FFFFFF);
    t = (t >> 26) + 2L * (x_0 * x_5 + x_1 * x_4 + x_2 * x_3) + 38L * (x_6 * x_9 + x_7 * x_8);
    
    x2._5 = (t & 0x1FFFFFF);
    t = (t >> 25) + 19L * (x_8 * x_8) + 2L * (x_0 * x_6 + x_2 * x_4 + x_3 * x_3) + 4L * (x_1 * x_5) + 76L * (x_7 * x_9);
    

    x2._6 = (t & 0x3FFFFFF);
    t = (t >> 26) + 2L * (x_0 * x_7 + x_1 * x_6 + x_2 * x_5 + x_3 * x_4) + 38L * (x_8 * x_9);
    
    x2._7 = (t & 0x1FFFFFF);
    t = (t >> 25) + x2._8;
    x2._8 = (t & 0x3FFFFFF);
    x2._9 += (t >> 26);
    return x2;
  }
  
  private static final void recip(Nxt.Curve25519.long10 y, Nxt.Curve25519.long10 x, int sqrtassist)
  {
    Nxt.Curve25519.long10 t0 = new Nxt.Curve25519.long10();
    Nxt.Curve25519.long10 t1 = new Nxt.Curve25519.long10();
    Nxt.Curve25519.long10 t2 = new Nxt.Curve25519.long10();
    Nxt.Curve25519.long10 t3 = new Nxt.Curve25519.long10();
    Nxt.Curve25519.long10 t4 = new Nxt.Curve25519.long10();
    

    sqr(t1, x);
    sqr(t2, t1);
    sqr(t0, t2);
    mul(t2, t0, x);
    mul(t0, t2, t1);
    sqr(t1, t0);
    mul(t3, t1, t2);
    
    sqr(t1, t3);
    sqr(t2, t1);
    sqr(t1, t2);
    sqr(t2, t1);
    sqr(t1, t2);
    mul(t2, t1, t3);
    sqr(t1, t2);
    sqr(t3, t1);
    for (int i = 1; i < 5; i++)
    {
      sqr(t1, t3);
      sqr(t3, t1);
    }
    mul(t1, t3, t2);
    sqr(t3, t1);
    sqr(t4, t3);
    for (i = 1; i < 10; i++)
    {
      sqr(t3, t4);
      sqr(t4, t3);
    }
    mul(t3, t4, t1);
    for (i = 0; i < 5; i++)
    {
      sqr(t1, t3);
      sqr(t3, t1);
    }
    mul(t1, t3, t2);
    sqr(t2, t1);
    sqr(t3, t2);
    for (i = 1; i < 25; i++)
    {
      sqr(t2, t3);
      sqr(t3, t2);
    }
    mul(t2, t3, t1);
    sqr(t3, t2);
    sqr(t4, t3);
    for (i = 1; i < 50; i++)
    {
      sqr(t3, t4);
      sqr(t4, t3);
    }
    mul(t3, t4, t2);
    for (i = 0; i < 25; i++)
    {
      sqr(t4, t3);
      sqr(t3, t4);
    }
    mul(t2, t3, t1);
    sqr(t1, t2);
    sqr(t2, t1);
    if (sqrtassist != 0)
    {
      mul(y, x, t2);
    }
    else
    {
      sqr(t1, t2);
      sqr(t2, t1);
      sqr(t1, t2);
      mul(y, t1, t0);
    }
  }
  
  private static final int is_negative(Nxt.Curve25519.long10 x)
  {
    return (int)(((is_overflow(x)) || (x._9 < 0L) ? 1 : 0) ^ x._0 & 1L);
  }
  
  private static final void sqrt(Nxt.Curve25519.long10 x, Nxt.Curve25519.long10 u)
  {
    Nxt.Curve25519.long10 v = new Nxt.Curve25519.long10();Nxt.Curve25519.long10 t1 = new Nxt.Curve25519.long10();Nxt.Curve25519.long10 t2 = new Nxt.Curve25519.long10();
    add(t1, u, u);
    recip(v, t1, 1);
    sqr(x, v);
    mul(t2, t1, x);
    t2._0 -= 1L;
    mul(t1, v, t2);
    mul(x, u, t1);
  }
  
  private static final void mont_prep(Nxt.Curve25519.long10 t1, Nxt.Curve25519.long10 t2, Nxt.Curve25519.long10 ax, Nxt.Curve25519.long10 az)
  {
    add(t1, ax, az);
    sub(t2, ax, az);
  }
  
  private static final void mont_add(Nxt.Curve25519.long10 t1, Nxt.Curve25519.long10 t2, Nxt.Curve25519.long10 t3, Nxt.Curve25519.long10 t4, Nxt.Curve25519.long10 ax, Nxt.Curve25519.long10 az, Nxt.Curve25519.long10 dx)
  {
    mul(ax, t2, t3);
    mul(az, t1, t4);
    add(t1, ax, az);
    sub(t2, ax, az);
    sqr(ax, t1);
    sqr(t1, t2);
    mul(az, t1, dx);
  }
  
  private static final void mont_dbl(Nxt.Curve25519.long10 t1, Nxt.Curve25519.long10 t2, Nxt.Curve25519.long10 t3, Nxt.Curve25519.long10 t4, Nxt.Curve25519.long10 bx, Nxt.Curve25519.long10 bz)
  {
    sqr(t1, t3);
    sqr(t2, t4);
    mul(bx, t1, t2);
    sub(t2, t1, t2);
    mul_small(bz, t2, 121665L);
    add(t1, t1, bz);
    mul(bz, t1, t2);
  }
  
  private static final void x_to_y2(Nxt.Curve25519.long10 t, Nxt.Curve25519.long10 y2, Nxt.Curve25519.long10 x)
  {
    sqr(t, x);
    mul_small(y2, x, 486662L);
    add(t, t, y2);
    t._0 += 1L;
    mul(y2, t, x);
  }
  
  private static final void core(byte[] Px, byte[] s, byte[] k, byte[] Gx)
  {
    Nxt.Curve25519.long10 dx = new Nxt.Curve25519.long10();
    Nxt.Curve25519.long10 t1 = new Nxt.Curve25519.long10();
    Nxt.Curve25519.long10 t2 = new Nxt.Curve25519.long10();
    Nxt.Curve25519.long10 t3 = new Nxt.Curve25519.long10();
    Nxt.Curve25519.long10 t4 = new Nxt.Curve25519.long10();
    
    Nxt.Curve25519.long10[] x = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
    Nxt.Curve25519.long10[] z = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
    if (Gx != null) {
      unpack(dx, Gx);
    } else {
      set(dx, 9);
    }
    set(x[0], 1);
    set(z[0], 0);
    

    cpy(x[1], dx);
    set(z[1], 1);
    for (int i = 32; i-- != 0;)
    {
      if (i == 0) {
        i = 0;
      }
      for (j = 8; j-- != 0;)
      {
        int bit1 = (k[i] & 0xFF) >> j & 0x1;
        int bit0 = (k[i] & 0xFF ^ 0xFFFFFFFF) >> j & 0x1;
        Nxt.Curve25519.long10 ax = x[bit0];
        Nxt.Curve25519.long10 az = z[bit0];
        Nxt.Curve25519.long10 bx = x[bit1];
        Nxt.Curve25519.long10 bz = z[bit1];
        


        mont_prep(t1, t2, ax, az);
        mont_prep(t3, t4, bx, bz);
        mont_add(t1, t2, t3, t4, ax, az, dx);
        mont_dbl(t1, t2, t3, t4, bx, bz);
      }
    }
    int j;
    recip(t1, z[0], 0);
    mul(dx, x[0], t1);
    pack(dx, Px);
    if (s != null)
    {
      x_to_y2(t2, t1, dx);
      recip(t3, z[1], 0);
      mul(t2, x[1], t3);
      add(t2, t2, dx);
      t2._0 += 486671L;
      dx._0 -= 9L;
      sqr(t3, dx);
      mul(dx, t2, t3);
      sub(dx, dx, t1);
      dx._0 -= 39420360L;
      mul(t1, dx, BASE_R2Y);
      if (is_negative(t1) != 0) {
        cpy32(s, k);
      } else {
        mula_small(s, ORDER_TIMES_8, 0, k, 32, -1);
      }
      byte[] temp1 = new byte[32];
      byte[] temp2 = new byte[64];
      byte[] temp3 = new byte[64];
      cpy32(temp1, ORDER);
      cpy32(s, egcd32(temp2, temp3, s, temp1));
      if ((s[31] & 0x80) != 0) {
        mula_small(s, s, 0, ORDER, 32, 1);
      }
