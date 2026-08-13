package com.re.skin;

/** UI进程 <-> root进程: 内存读写(非attach) + 皮肤应用 */
interface IMutual {
    /** 绑定游戏进程(找pid+open mem), 返回pid或-1 */
    long attach(String gamePackage);
    /** 内存搜索值(找装扮基址A), 返回匹配数 */
    int scanValue(int value);
    /** 获取第index个匹配地址 */
    long getScanResult(int index);
    /** 应用皮肤: 写 baseA(头) + baseA+4(脸) + baseA+8(身) */
    boolean applySkin(long baseA, int head, int face, int body);
    /** 读4字节(验证) */
    long readInt(long addr);
    /** 自动定位装扮基址A: 搜头ID+结构验证(脸A+8,身A+0x14) */
    long findSkinBase(int headId, int faceId, int bodyId);
    /** 重新绑定(切换游戏) */
    void detach();
}
