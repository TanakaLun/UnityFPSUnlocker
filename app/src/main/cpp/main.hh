#ifndef MAIN_HEADER
#define MAIN_HEADER

#include <string>

#include "utility/config.hh"
#include "utility/logger.hh"

class FPSUnlockerManager {
public:
    static FPSUnlockerManager& GetInstance();
    
    void Initialize();
    void SetConfig(const ConfigValue& config);
    void SetConfigForPackage(const std::string& package_name, const ConfigValue& config);
    ConfigValue GetCurrentConfig() const;
    
private:
    FPSUnlockerManager() = default;
    ConfigValue current_config_;
};

#endif // main.hh