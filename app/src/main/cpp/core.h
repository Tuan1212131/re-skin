#pragma once
#include <cstdint>
#include <cstddef>

namespace skin {

long attach(const char* gamePkg);
void detach();
long currentPid();
bool memRead(uintptr_t addr, void* dst, size_t len);
bool memWrite(uintptr_t addr, const void* src, size_t len);
int scanValue(int value);
uintptr_t getScanResult(int index);
int scanResultCount();
bool applySkin(uintptr_t baseA, int head, int face, int body);

// 自动定位装扮基址A: 搜头ID, 验证 A+8=脸ID 且 A+0x14=身ID (结构验证)
uintptr_t findSkinBase(int headId, int faceId, int bodyId);

// v6: 搜"身"c2(身), 验证密集签名 [..-12=4,-8=0,-4=脸c1,0=身c2,+4=部件3c3,+8=0]; 返回身地址
uintptr_t findDense(int c1, int c2, int c3);
// v6 写密集块: 身-4=c1, 身=c2, 身+4=c3
bool applyDense(uintptr_t center, int c1, int c2, int c3);

} // namespace skin
