package io.github.hexstr.UnityFPSUnlocker;

public class ConfigValue {
    public int delay;
    public int fps;
    public boolean modOpcode;
    public float scale;
    
    public ConfigValue(int delay, int fps, boolean modOpcode, float scale) {
        this.delay = delay;
        this.fps = fps;
        this.modOpcode = modOpcode;
        this.scale = scale;
    }
}