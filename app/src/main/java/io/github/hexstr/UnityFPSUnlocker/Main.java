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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Main implements IXposedHookLoadPackage {
    
    static {
        System.loadLibrary("UnityFPSUnlocker");
    }
    
    // Native方法声明
    public static native void nativeInitialize();
    public static native void nativeSetConfig(int delay, int fps, boolean modOpcode, float scale);
    public static native void nativeSetConfigForPackage(String packageName, int delay, int fps, boolean modOpcode, float scale);
    public static native int nativeGetDelay();
    public static native int nativeGetFPS();
    public static native boolean nativeGetModOpcode();
    public static native float nativeGetScale();
    public static native void nativeStart(int delay, int fps, boolean modOpcode, float scale);
    public static native void nativeStartForPackage(String packageName, int delay, int fps, boolean modOpcode, float scale);
    
    // 悬浮窗相关变量
    private View floatingView = null;
    private FrameLayout hostDecorView = null;
    private int screenWidth = 0;
    private int screenHeight = 0;
    private boolean isFloatingWindowCreated = false;
    
    // 当前包名和配置
    private String currentPackageName = "";
    private int currentDelay = 3;
    private int currentFPS = 120;
    private boolean currentModOpcode = true;
    private float currentScale = 1.0f;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("UnityFPSUnlocker: Loading for package: " + lpparam.packageName);
        
        try {
            // 初始化Native层
            nativeInitialize();
            
            // 从Native层获取当前配置
            currentDelay = nativeGetDelay();
            currentFPS = nativeGetFPS();
            currentModOpcode = nativeGetModOpcode();
            currentScale = nativeGetScale();
            
            // Hook Activity的onResume方法
            XposedHelpers.findAndHookMethod("android.app.Activity", lpparam.classLoader, 
                "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (isFloatingWindowCreated) {
                        return;
                    }
                    
                    Activity activity = (Activity) param.thisObject;
                    currentPackageName = activity.getPackageName();
                    
                    XposedBridge.log("UnityFPSUnlocker: Activity resumed: " + activity.getClass().getName() + 
                                    ", Package: " + currentPackageName);
                    
                    // 延迟1秒确保Activity完全初始化
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                attachToActivity(activity);
                                isFloatingWindowCreated = true;
                                
                                // 应用当前配置到当前包
                                applyCurrentConfig();
                            } catch (Exception e) {
                                XposedBridge.log("UnityFPSUnlocker Error: " + e.getMessage());
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
            hostDecorView = (FrameLayout) activity.getWindow().getDecorView();
            getScreenSize(activity);
            createFloatingWindow(activity);
            
            XposedBridge.log("UnityFPSUnlocker: Floating window attached successfully for package: " + currentPackageName);
            
        } catch (Exception e) {
            XposedBridge.log("UnityFPSUnlocker Error: " + e.getMessage());
        }
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
            // 默认值
            screenWidth = 1920;
            screenHeight = 1080;
        }
    }
    
    private void createFloatingWindow(final Context context) {
        if (floatingView != null) return;
        
        try {
            // 计算悬浮窗尺寸 - 横屏宽度的1/3，高度的1/2
            final int floatingWidth = screenWidth / 3;
            final int floatingHeight = screenHeight / 2;
            
            LinearLayout mainLayout = new LinearLayout(context);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            
            // 设置背景
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
            
            XposedBridge.log("UnityFPSUnlocker: Floating window created successfully - Size: " + 
                floatingWidth + "x" + floatingHeight + " for package: " + currentPackageName);
            
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
            dpToPx(context, 50)
        );
        titleBar.setLayoutParams(titleParams);
        
        // 标题文本
        TextView titleText = new TextView(context);
        titleText.setText("FPS解锁设置 - " + currentPackageName);
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
            dpToPx(context, 50),
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
        
        // 添加FPS解锁控制项
        createDelayControl(context, contentLayout);
        createFPSControl(context, contentLayout);
        createModOpcodeControl(context, contentLayout);
        createScaleControl(context, contentLayout);
        createApplyButton(context, contentLayout);
        
        parent.addView(scrollView);
    }
    
    private void createDelayControl(Context context, LinearLayout parent) {
        LinearLayout delayLayout = new LinearLayout(context);
        delayLayout.setOrientation(LinearLayout.VERTICAL);
        
        final TextView delayText = new TextView(context);
        delayText.setText("延迟启动: " + currentDelay + "秒");
        delayText.setTextColor(Color.WHITE);
        delayText.setTextSize(14);
        
        SeekBar delaySeekBar = new SeekBar(context);
        delaySeekBar.setMax(10); // 0-10秒
        delaySeekBar.setProgress(currentDelay);
        
        delaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentDelay = progress;
                delayText.setText("延迟启动: " + currentDelay + "秒");
            }
            
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        delayLayout.addView(delayText);
        delayLayout.addView(delaySeekBar);
        parent.addView(delayLayout);
    }
    
    private void createFPSControl(Context context, LinearLayout parent) {
        LinearLayout fpsLayout = new LinearLayout(context);
        fpsLayout.setOrientation(LinearLayout.VERTICAL);
        
        final TextView fpsText = new TextView(context);
        fpsText.setText("目标帧率: " + currentFPS + " FPS");
        fpsText.setTextColor(Color.WHITE);
        fpsText.setTextSize(14);
        
        SeekBar fpsSeekBar = new SeekBar(context);
        fpsSeekBar.setMax(240 - 30); // 30-240 FPS
        fpsSeekBar.setProgress(currentFPS - 30);
        
        fpsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentFPS = progress + 30;
                fpsText.setText("目标帧率: " + currentFPS + " FPS");
            }
            
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        fpsLayout.addView(fpsText);
        fpsLayout.addView(fpsSeekBar);
        parent.addView(fpsLayout);
    }
    
    private void createModOpcodeControl(Context context, LinearLayout parent) {
        LinearLayout opcodeLayout = new LinearLayout(context);
        opcodeLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        TextView opcodeText = new TextView(context);
        opcodeText.setText("修改Opcode: ");
        opcodeText.setTextColor(Color.WHITE);
        opcodeText.setTextSize(14);
        
        Switch opcodeSwitch = new Switch(context);
        opcodeSwitch.setChecked(currentModOpcode);
        opcodeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentModOpcode = isChecked;
        });
        
        opcodeLayout.addView(opcodeText);
        opcodeLayout.addView(opcodeSwitch);
        parent.addView(opcodeLayout);
    }
    
    private void createScaleControl(Context context, LinearLayout parent) {
        LinearLayout scaleLayout = new LinearLayout(context);
        scaleLayout.setOrientation(LinearLayout.VERTICAL);
        
        final TextView scaleText = new TextView(context);
        scaleText.setText("分辨率缩放: " + String.format("%.1f", currentScale) + "x");
        scaleText.setTextColor(Color.WHITE);
        scaleText.setTextSize(14);
        
        SeekBar scaleSeekBar = new SeekBar(context);
        scaleSeekBar.setMax(20); // 0.5x - 2.5x, 步长0.1
        scaleSeekBar.setProgress((int)((currentScale - 0.5f) * 10));
        
        scaleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentScale = 0.5f + progress * 0.1f;
                scaleText.setText("分辨率缩放: " + String.format("%.1f", currentScale) + "x");
            }
            
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        scaleLayout.addView(scaleText);
        scaleLayout.addView(scaleSeekBar);
        parent.addView(scaleLayout);
    }
    
    private void createApplyButton(Context context, LinearLayout parent) {
        TextView applyButton = new TextView(context);
        applyButton.setText("应用设置到当前应用");
        applyButton.setTextColor(Color.WHITE);
        applyButton.setTextSize(16);
        applyButton.setGravity(Gravity.CENTER);
        applyButton.setBackgroundColor(0xFF4CAF50);
        applyButton.setPadding(0, dpToPx(context, 12), 0, dpToPx(context, 12));
        
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyCurrentConfig();
            }
        });
        
        parent.addView(applyButton);
    }
    
    private void applyCurrentConfig() {
        try {
            nativeSetConfigForPackage(currentPackageName, currentDelay, currentFPS, currentModOpcode, currentScale);
            XposedBridge.log("UnityFPSUnlocker: Config applied for package: " + currentPackageName + 
                " - Delay: " + currentDelay + ", FPS: " + currentFPS + 
                ", ModOpcode: " + currentModOpcode + ", Scale: " + currentScale);
        } catch (Exception e) {
            XposedBridge.log("UnityFPSUnlocker Error applying config: " + e.getMessage());
        }
    }
    
    private void setupTouchListener(final View view, final FrameLayout.LayoutParams params) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isDragging = false;
            private long pressStartTime = 0;
            private static final long LONG_PRESS_THRESHOLD = 300; // 长按阈值300毫秒
            
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
                        return true; // 返回true表示我们想处理后续事件
                        
                    case MotionEvent.ACTION_MOVE:
                        long currentTime = System.currentTimeMillis();
                        
                        // 检查是否达到长按时间阈值
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
                XposedBridge.log("UnityFPSUnlocker: Floating window removed for package: " + currentPackageName);
            }
        } catch (Exception e) {
            XposedBridge.log("UnityFPSUnlocker Error removing window: " + e.getMessage());
        }
    }
    
    private int dpToPx(Context context, int dp) {
        return (int)(dp * context.getResources().getDisplayMetrics().density);
    }
}