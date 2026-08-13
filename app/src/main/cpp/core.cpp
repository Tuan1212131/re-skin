// =====================================================================
// 皮肤工具 Native 核心: 非attach内存读写 + 搜索 + 应用皮肤
//   - 直接 open(/proc/<pid>/mem) + pread/pwrite (不用ptrace, 防游戏检测)
//   - 搜索值定位角色装扮基址A
//   - applySkin: 写 A(头) A+4(脸) A+8(身)
// =====================================================================
#include "core.h"
#include <android/log.h>
#include <dirent.h>
#include <fcntl.h>
#include <unistd.h>
#include <cstdio>
#include <cstring>
#include <cstdlib>

#define LOGTAG "skin-core"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOGTAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOGTAG, __VA_ARGS__)

namespace skin {

static long   g_pid   = -1;
static int    g_memFd = -1;

// 搜索结果
static uintptr_t g_results[4096];
static int g_resultCount = 0;

static long findPid(const char* pkg) {
    DIR* dir = opendir("/proc");
    if (!dir) return -1;
    struct dirent* ent;
    long found = -1;
    while ((ent = readdir(dir)) != nullptr) {
        if (ent->d_name[0] < '0' || ent->d_name[0] > '9') continue;
        long pid = atol(ent->d_name);
        char path[64], cmd[512];
        snprintf(path, sizeof path, "/proc/%ld/cmdline", pid);
        int fd = open(path, O_RDONLY);
        if (fd < 0) continue;
        ssize_t n = read(fd, cmd, sizeof cmd - 1);
        close(fd);
        if (n <= 0) continue;
        cmd[n] = 0;
        if (strncmp(cmd, pkg, strlen(pkg)) == 0) { found = pid; break; }
    }
    closedir(dir);
    return found;
}

long attach(const char* gamePkg) {
    if (g_pid > 0 && g_memFd >= 0) return g_pid;
    if (!gamePkg || !*gamePkg) return -1;
    long pid = findPid(gamePkg);
    if (pid < 0) { LOGE("attach: %s 未运行", gamePkg); return -1; }
    char memPath[64];
    snprintf(memPath, sizeof memPath, "/proc/%ld/mem", pid);
    int fd = open(memPath, O_RDWR);          // 非attach, 直接open
    if (fd < 0) { LOGE("attach: open %s 失败", memPath); return -1; }
    g_pid = pid;
    g_memFd = fd;
    g_resultCount = 0;
    LOGI("attach: pid=%ld", pid);
    return pid;
}

void detach() {
    if (g_memFd >= 0) { close(g_memFd); g_memFd = -1; }
    g_pid = -1;
    g_resultCount = 0;
}

long currentPid() { return g_pid; }

bool memRead(uintptr_t addr, void* dst, size_t len) {
    if (g_memFd < 0 || addr == 0) return false;
    ssize_t n = pread(g_memFd, dst, len, (off_t)addr);
    return n == (ssize_t)len;
}

bool memWrite(uintptr_t addr, const void* src, size_t len) {
    if (g_memFd < 0 || addr == 0) return false;
    ssize_t n = pwrite(g_memFd, src, len, (off_t)addr);
    return n == (ssize_t)len;
}

// 内存搜索指定int值, 收集地址
int scanValue(int value) {
    if (g_memFd < 0) return 0;
    g_resultCount = 0;
    char mapPath[64];
    snprintf(mapPath, sizeof mapPath, "/proc/%ld/maps", g_pid);
    FILE* f = fopen(mapPath, "r");
    if (!f) return 0;
    char line[512];
    unsigned char buf[4096];
    while (fgets(line, sizeof line, f)) {
        unsigned long start, end;
        char perms[8] = "";
        if (sscanf(line, "%lx-%lx %7s", &start, &end, perms) != 3) continue;
        if (!strchr(perms, 'r')) continue;
        if (strstr(line, "vmem") || strstr(line, "vsyscall") || strstr(line, "/dev/")) continue;
        if (strstr(line, "dalvik") || strstr(line, "jit") || strstr(line, "oat")) continue;
        for (unsigned long addr = start; addr < end; addr += 4096) {
            size_t len = (end - addr < 4096) ? (size_t)(end - addr) : 4096;
            if (pread(g_memFd, buf, len, (off_t)addr) != (ssize_t)len) continue;
            for (size_t i = 0; i + 4 <= len && g_resultCount < 4096; i++) {
                int v;
                memcpy(&v, buf + i, 4);
                if (v == value) {
                    g_results[g_resultCount++] = addr + i;
                }
            }
            if (g_resultCount >= 4096) break;
        }
        if (g_resultCount >= 4096) break;
    }
    fclose(f);
    LOGI("scanValue(%d) 找到 %d 个", value, g_resultCount);
    return g_resultCount;
}

uintptr_t getScanResult(int index) {
    if (index < 0 || index >= g_resultCount) return 0;
    return g_results[index];
}

int scanResultCount() { return g_resultCount; }

// 应用皮肤: 写 A(头) A+8(脸) A+0x14(身)
// 依据 RoleClothInfo 数组结构实测:
//   part[1]=头, part[3]=脸, part[6]=身 (int槽位, 非连续+4)
bool applySkin(uintptr_t baseA, int head, int face, int body) {
    if (g_memFd < 0 || baseA == 0) return false;
    bool ok = memWrite(baseA + 0,    &head, 4)   // 头
           && memWrite(baseA + 8,    &face, 4)   // 脸
           && memWrite(baseA + 0x14, &body, 4);  // 身
    LOGI("applySkin: A=%p head=%d face=%d body=%d -> %s",
         (void*)baseA, head, face, body, ok ? "OK" : "FAIL");
    return ok;
}

} // namespace skin
