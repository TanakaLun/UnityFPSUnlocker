package io.github.hexstr.UnityFPSUnlocker;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigManager {
    private SharedPreferences prefs;
    private String packageName;
    
    public ConfigManager(Context context, String packageName) {
        this.packageName = packageName;
        this.prefs = context.getSharedPreferences("unity_fps_unlocker_config", Context.MODE_PRIVATE);
    }
    
    public ConfigValue getCurrentConfig() {
        return new ConfigValue(
            prefs.getInt(packageName + "_delay", 5),
            prefs.getInt(packageName + "_fps", 90),
            prefs.getBoolean(packageName + "_mod_opcode", false),
            prefs.getFloat(packageName + "_scale", 1.0f)
        );
    }
    
    public void updateConfig(ConfigValue config) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(packageName + "_delay", config.delay);
        editor.putInt(packageName + "_fps", config.fps);
        editor.putBoolean(packageName + "_mod_opcode", config.modOpcode);
        editor.putFloat(packageName + "_scale", config.scale);
        editor.apply();
    }
}