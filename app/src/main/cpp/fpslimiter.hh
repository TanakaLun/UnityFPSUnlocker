#ifndef FPSLIMITER_HEADER
#define FPSLIMITER_HEADER

#include "utility/config.hh"

namespace FPSLimiter {
    void Start(const ConfigValue& config);
    bool IsRunning();
    void Stop();
} // namespace FPSLimiter

#endif // fpslimiter.hh