package com.re.skin;

import android.content.Intent;
import android.os.IBinder;

import com.topjohnwu.superuser.ipc.RootService;

/** root进程服务: 内存读写(非attach) + 皮肤应用 */
public class AIDLService extends RootService {
    static {
        try { System.loadLibrary("main"); } catch (UnsatisfiedLinkError e) { }
    }
    @Override public IBinder onBind(Intent intent) {
        return new IMutual.Stub() {
            @Override public long attach(String pkg) { return NativeBridge.attach(pkg); }
            @Override public int scanValue(int value) { return NativeBridge.scanValue(value); }
            @Override public long getScanResult(int index) { return NativeBridge.getScanResult(index); }
            @Override public boolean applySkin(long a, int h, int f, int b) {
                return NativeBridge.applySkin(a, h, f, b);
            }
            @Override public long readInt(long addr) { return NativeBridge.readInt(addr); }
            @Override public long findSkinBase(int h, int f, int b) {
                return NativeBridge.findSkinBase(h, f, b);
            }
            @Override public void detach() { NativeBridge.detach(); }
        };
    }
}
