#ifndef FPSLIMITER_HEADER
#define FPSLIMITER_HEADER

#include "utility/config.hh"

namespace FPSLimiter {
    void Start(const ConfigValue&);
    void UpdateConfig(const ConfigValue& new_config);
    void SetFrameRate(int fps);
} // namespace FPSLimiter

#endif // fpslimiter.hh