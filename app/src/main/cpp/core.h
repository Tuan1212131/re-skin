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

} // namespace skin
