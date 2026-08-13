package com.re.skin;

public final class IPCService {
    private static IMutual ipc;
    public static void initIPC(IMutual m) { ipc = m; }
    public static IMutual getIPC() { return ipc; }
    public static boolean isConnect() { return ipc != null; }
}
