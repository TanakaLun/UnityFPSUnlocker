#ifndef MAIN_HEADER
#define MAIN_HEADER

#include "utility/config.hh"
#include "utility/logger.hh"

class FPSUnlockerManager {
public:
    static FPSUnlockerManager& GetInstance();
    
    void Initialize();
    void SetConfig(const ConfigValue& config);
    ConfigValue GetCurrentConfig() const;
    bool ShouldEnableForApp();
    
private:
    FPSUnlockerManager() = default;
    ConfigValue current_config_;
};

#endif // main.hh