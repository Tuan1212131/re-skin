package com.re.skin;

/** JNI桥接 (root进程加载) */
public final class NativeBridge {
    static { System.loadLibrary("main"); }
    private NativeBridge() {}
    public static native long attach(String gamePackage);
    public static native void detach();
    public static native int scanValue(int value);
    public static native long getScanResult(int index);
    public static native int scanResultCount();
    public static native boolean applySkin(long baseA, int head, int face, int body);
    public static native long readInt(long addr);
    /** 自动定位: 搜头ID, 结构验证(A+8=脸, A+0x14=身), 返回装扮基址A */
    public static native long findSkinBase(int head, int face, int body);
    public static native long deepLocate(int c1, int c2, int c3);
    public static native boolean applyDeep(long center, int c1, int c2, int c3);
}
