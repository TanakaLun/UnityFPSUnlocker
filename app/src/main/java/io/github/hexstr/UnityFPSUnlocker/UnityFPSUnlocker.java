package io.github.hexstr.UnityFPSUnlocker;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class UnityFPSUnlocker implements IXposedHookLoadPackage {
    
    static {
        System.loadLibrary("UnityFPSUnlocker");
    }
    
    // Native 方法声明
    public static native void startFPSLimiter();
    public static native void updateConfig(int delay, int fps, boolean modOpcode, float scale);
    public static native void setFrameRate(int fps);
    
    // 配置参数
    private int currentFPS = 90;
    private int currentDelay = 5;
    private boolean currentModOpcode = false;
    private float currentScale = 1.0f;
    
    private View floatingView = null;
    private FrameLayout hostDecorView = null;
    private int screenWidth = 0;
    private int screenHeight = 0;
    private boolean isFloatingWindowCreated = false;
    private ConfigManager configManager;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 只处理目标包名（这里以原神为例，您可以根据需要修改）
        if (!lpparam.packageName.equals("com.miHoYo.Yuanshen") && 
            !lpparam.packageName.equals("com.miHoYo.GenshinImpact")) {
            return;
        }
        
        XposedBridge.log("UnityFPSUnlocker: Loading for package: " + lpparam.packageName);
        
        try {
            // Hook Activity的onResume方法
            XposedHelpers.findAndHookMethod("android.app.Activity", lpparam.classLoader, 
                "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (isFloatingWindowCreated) {
                        return;
                    }
                    
                    Activity activity = (Activity) param.thisObject;
                    XposedBridge.log("UnityFPSUnlocker: Activity resumed: " + activity.getClass().getName());
                    
                    // 初始化配置管理器
                    configManager = new ConfigManager(activity, lpparam.packageName);
                    
                    // 加载配置
                    ConfigValue config = configManager.getCurrentConfig();
                    currentDelay = config.delay;
                    currentFPS = config.fps;
                    currentModOpcode = config.modOpcode;
                    currentScale = config.scale;
                    
                    // 延迟1秒确保Activity完全初始化
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                attachToActivity(activity);
                                isFloatingWindowCreated = true;
                                
                                // 启动原生FPS限制器
                                startNativeFPSLimiter();
                            } catch (Exception e) {
                                XposedBridge.log("UnityFPSUnlocker Error attaching to activity: " + e.getMessage());
                            }
                        }
                    }, 1000);
                }
            });
            
        } catch (Throwable e) {
            XposedBridge.log("UnityFPSUnlocker Hook Error: " + e.getMessage());
        }
    }
    
    private void attachToActivity(Activity activity) {
        try {
            // 获取宿主Activity的DecorView
            hostDecorView = (FrameLayout) activity.getWindow().getDecorView();
            
            // 获取屏幕尺寸
            getScreenSize(activity);
            
            // 创建悬浮窗
            createFloatingWindow(activity);
            
            XposedBridge.log("UnityFPSUnlocker: Floating window attached to activity successfully");
            
        } catch (Exception e) {
            XposedBridge.log("UnityFPSUnlocker Error attaching to activity: " + e.getMessage());
        }
    }
    
    private void startNativeFPSLimiter() {
        new Thread(() -> {
            try {
                // 应用当前配置
                updateConfig(currentDelay, currentFPS, currentModOpcode, currentScale);
                
                // 启动FPS限制器
                startFPSLimiter();
                
                XposedBridge.log("UnityFPSUnlocker: Native FPS limiter started with FPS: " + currentFPS);
            } catch (Exception e) {
                XposedBridge.log("UnityFPSUnlocker Error starting native limiter: " + e.getMessage());
            }
        }).start();
    }
    
    private void getScreenSize(Context context) {
        try {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            
            screenWidth = Math.max(metrics.widthPixels, metrics.heightPixels);
            screenHeight = Math.min(metrics.widthPixels, metrics.heightPixels);
            
            XposedBridge.log("UnityFPSUnlocker: Screen size - " + screenWidth + "x" + screenHeight);
            
        } catch (Exception e) {
            XposedBridge.log("UnityFPSUnlocker Error getting screen size: " + e.getMessage());
            screenWidth = 1920;
            screenHeight = 1080;
        }
    }
    
    private void createFloatingWindow(final Context context) {
        if (floatingView != null) return;
        
        try {
            // 计算悬浮窗尺寸
            final int floatingWidth = screenWidth / 3;
            final int floatingHeight = screenHeight / 2;
            
            // 创建主容器
            LinearLayout mainLayout = new LinearLayout(context);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setBackgroundColor(0xCC1A1A1A);
            
            // 创建布局参数
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                floatingWidth,
                floatingHeight
            );
            layoutParams.gravity = Gravity.TOP | Gravity.END;
            layoutParams.leftMargin = 20;
            layoutParams.topMargin = 100;
            
            // 创建标题栏
            createTitleBar(context, mainLayout);
            
            // 创建内容区域
            createContentArea(context, mainLayout);
            
            // 设置触摸监听实现拖动
            setupTouchListener(mainLayout, layoutParams);
            
            floatingView = mainLayout;
            hostDecorView.addView(floatingView, layoutParams);
            
            XposedBridge.log("UnityFPSUnlocker: Floating window created successfully");
            
        } catch (Exception e) {
            XposedBridge.log("UnityFPSUnlocker Error creating window: " + e.getMessage());
        }
    }
    
    private void createTitleBar(Context context, LinearLayout parent) {
        // 标题栏容器
        LinearLayout titleBar = new LinearLayout(context);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setBackgroundColor(0xAA2D2D2D);
        
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(context, 40)
        );
        titleBar.setLayoutParams(titleParams);
        
        // 标题文本
        TextView titleText = new TextView(context);
        titleText.setText("FPS Unlocker");
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(14);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        textParams.gravity = Gravity.CENTER_VERTICAL;
        textParams.leftMargin = dpToPx(context, 12);
        titleText.setLayoutParams(textParams);
        
        // 关闭按钮
        TextView closeButton = new TextView(context);
        closeButton.setText("×");
        closeButton.setTextColor(Color.WHITE);
        closeButton.setTextSize(20);
        
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
            dpToPx(context, 40),
            LinearLayout.LayoutParams.MATCH_PARENT
        );
        closeButton.setLayoutParams(closeParams);
        closeButton.setGravity(Gravity.CENTER);
        
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeFloatingWindow();
            }
        });
        
        titleBar.addView(titleText);
        titleBar.addView(closeButton);
        parent.addView(titleBar);
    }
    
    private void createContentArea(Context context, LinearLayout parent) {
        // 滚动容器
        ScrollView scrollView = new ScrollView(context);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        );
        scrollView.setLayoutParams(scrollParams);
        
        // 内容容器
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(
            dpToPx(context, 16),
            dpToPx(context, 12),
            dpToPx(context, 16),
            dpToPx(context, 12)
        );
        scrollView.addView(contentLayout);
        
        // 添加FPS控制
        createFPSControl(context, contentLayout);
        
        // 添加延迟控制
        createDelayControl(context, contentLayout);
        
        // 添加分辨率缩放控制
        createScaleControl(context, contentLayout);
        
        // 添加操作码修改开关
        createModOpcodeControl(context, contentLayout);
        
        // 添加应用按钮
        createApplyButton(context, contentLayout);
        
        parent.addView(scrollView);
    }
    
    private void createFPSControl(Context context, LinearLayout parent) {
        // FPS显示
        final TextView fpsText = new TextView(context);
        fpsText.setText("FPS: " + currentFPS);
        fpsText.setTextColor(Color.WHITE);
        fpsText.setTextSize(14);
        
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.bottomMargin = dpToPx(context, 8);
        fpsText.setLayoutParams(textParams);
        parent.addView(fpsText);
        
        // 滑条容器
        LinearLayout sliderContainer = new LinearLayout(context);
        sliderContainer.setOrientation(LinearLayout.HORIZONTAL);
        
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.bottomMargin = dpToPx(context, 16);
        sliderContainer.setLayoutParams(containerParams);
        
        // 最小值标签
        TextView minLabel = new TextView(context);
        minLabel.setText("30");
        minLabel.setTextColor(0xFFCCCCCC);
        minLabel.setTextSize(10);
        
        LinearLayout.LayoutParams minParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        minLabel.setLayoutParams(minParams);
        
        // FPS滑条
        SeekBar fpsSeekBar = new SeekBar(context);
        
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        seekParams.leftMargin = dpToPx(context, 8);
        seekParams.rightMargin = dpToPx(context, 8);
        fpsSeekBar.setLayoutParams(seekParams);
        
        // 设置滑条范围：30-240
        fpsSeekBar.setMax(210); // 240 - 30 = 210
        fpsSeekBar.setProgress(currentFPS - 30);
        
        fpsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentFPS = progress + 30;
                    fpsText.setText("FPS: " + currentFPS);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 最大值标签
        TextView maxLabel = new TextView(context);
        maxLabel.setText("240");
        maxLabel.setTextColor(0xFFCCCCCC);
        maxLabel.setTextSize(10);
        
        LinearLayout.LayoutParams maxParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        maxLabel.setLayoutParams(maxParams);
        
        sliderContainer.addView(minLabel);
        sliderContainer.addView(fpsSeekBar);
        sliderContainer.addView(maxLabel);
        parent.addView(sliderContainer);
    }
    
    private void createDelayControl(Context context, LinearLayout parent) {
        // 延迟显示
        final TextView delayText = new TextView(context);
        delayText.setText("启动延迟: " + currentDelay + "秒");
        delayText.setTextColor(Color.WHITE);
        delayText.setTextSize(12);
        
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.bottomMargin = dpToPx(context, 8);
        delayText.setLayoutParams(textParams);
        parent.addView(delayText);
        
        // 延迟滑条
        SeekBar delaySeekBar = new SeekBar(context);
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        seekParams.bottomMargin = dpToPx(context, 16);
        delaySeekBar.setLayoutParams(seekParams);
        
        // 设置滑条范围：0-30秒
        delaySeekBar.setMax(30);
        delaySeekBar.setProgress(currentDelay);
        
        delaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentDelay = progress;
                    delayText.setText("启动延迟: " + currentDelay + "秒");
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        parent.addView(delaySeekBar);
    }
    
    private void createScaleControl(Context context, LinearLayout parent) {
        // 缩放显示
        final TextView scaleText = new TextView(context);
        scaleText.setText("分辨率缩放: " + String.format("%.1f", currentScale) + "x");
        scaleText.setTextColor(Color.WHITE);
        scaleText.setTextSize(12);
        
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.bottomMargin = dpToPx(context, 8);
        scaleText.setLayoutParams(textParams);
        parent.addView(scaleText);
        
        // 缩放滑条
        SeekBar scaleSeekBar = new SeekBar(context);
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        seekParams.bottomMargin = dpToPx(context, 16);
        scaleSeekBar.setLayoutParams(seekParams);
        
        // 设置滑条范围：0.5x-2.0x (步进0.1)
        scaleSeekBar.setMax(15); // (2.0 - 0.5) / 0.1 = 15
        scaleSeekBar.setProgress((int)((currentScale - 0.5) * 10));
        
        scaleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentScale = 0.5f + (progress * 0.1f);
                    scaleText.setText("分辨率缩放: " + String.format("%.1f", currentScale) + "x");
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        parent.addView(scaleSeekBar);
    }
    
    private void createModOpcodeControl(Context context, LinearLayout parent) {
        LinearLayout switchContainer = new LinearLayout(context);
        switchContainer.setOrientation(LinearLayout.HORIZONTAL);
        
        TextView switchText = new TextView(context);
        switchText.setText("修改操作码");
        switchText.setTextColor(Color.WHITE);
        switchText.setTextSize(12);
        
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        switchText.setLayoutParams(textParams);
        
        Button toggleButton = new Button(context);
        toggleButton.setText(currentModOpcode ? "开启" : "关闭");
        toggleButton.setTextSize(10);
        toggleButton.setAllCaps(false);
        
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentModOpcode = !currentModOpcode;
                toggleButton.setText(currentModOpcode ? "开启" : "关闭");
            }
        });
        
        switchContainer.addView(switchText);
        switchContainer.addView(toggleButton);
        parent.addView(switchContainer);
    }
    
    private void createApplyButton(Context context, LinearLayout parent) {
        Button applyButton = new Button(context);
        applyButton.setText("应用设置");
        applyButton.setTextSize(12);
        
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.topMargin = dpToPx(context, 8);
        applyButton.setLayoutParams(buttonParams);
        
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applySettings();
            }
        });
        
        parent.addView(applyButton);
    }
    
    private void applySettings() {
        try {
            // 保存配置
            ConfigValue newConfig = new ConfigValue(currentDelay, currentFPS, currentModOpcode, currentScale);
            configManager.updateConfig(newConfig);
            
            // 更新原生配置
            updateConfig(currentDelay, currentFPS, currentModOpcode, currentScale);
            
            // 实时更新帧率
            setFrameRate(currentFPS);
            
            XposedBridge.log("UnityFPSUnlocker: Settings applied - FPS: " + currentFPS + 
                ", Delay: " + currentDelay + ", Scale: " + currentScale);
            
            // 隐藏悬浮窗
            removeFloatingWindow();
            
        } catch (Exception e) {
            XposedBridge.log("UnityFPSUnlocker Error applying settings: " + e.getMessage());
        }
    }
    
    private void setupTouchListener(final View view, final FrameLayout.LayoutParams params) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isDragging = false;
            private long pressStartTime = 0;
            private static final long LONG_PRESS_THRESHOLD = 300;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.leftMargin;
                        initialY = params.topMargin;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        pressStartTime = System.currentTimeMillis();
                        isDragging = false;
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        long currentTime = System.currentTimeMillis();
                        
                        if (!isDragging && (currentTime - pressStartTime) >= LONG_PRESS_THRESHOLD) {
                            isDragging = true;
                        }
                        
                        if (isDragging) {
                            params.leftMargin = initialX + (int)(event.getRawX() - initialTouchX);
                            params.topMargin = initialY + (int)(event.getRawY() - initialTouchY);
                            
                            // 限制在屏幕范围内
                            params.leftMargin = Math.max(0, Math.min(screenWidth - view.getWidth(), params.leftMargin));
                            params.topMargin = Math.max(0, Math.min(screenHeight - view.getHeight(), params.topMargin));
                            
                            hostDecorView.updateViewLayout(view, params);
                            return true;
                        }
                        break;
                        
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isDragging = false;
                        pressStartTime = 0;
                        break;
                }
                return false;
            }
        });
    }
    
    private void removeFloatingWindow() {
        try {
            if (floatingView != null && hostDecorView != null) {
                hostDecorView.removeView(floatingView);
                floatingView = null;
                isFloatingWindowCreated = false;
                XposedBridge.log("UnityFPSUnlocker: Floating window removed");
            }
        } catch (Exception e) {
            XposedBridge.log("UnityFPSUnlocker Error removing window: " + e.getMessage());
        }
    }
    
    private int dpToPx(Context context, int dp) {
        return (int)(dp * context.getResources().getDisplayMetrics().density);
    }
}