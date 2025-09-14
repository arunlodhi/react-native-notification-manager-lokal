package io.lokal.notifications;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationManagerPackage implements ReactPackage {
    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();
        
        // Core notification modules
        modules.add(new NotificationManagerModule(reactContext));
        modules.add(new NotificationLimiterModule(reactContext));
        modules.add(new NotificationRefresherModule(reactContext));
        modules.add(new NotificationReCreatorModule(reactContext));
        
        // Additional modules for complete functionality
        modules.add(new NotificationRefreshAlarmManagerModule(reactContext));
        modules.add(new LocalNotificationManagerModule(reactContext));
        
        return modules;
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }
}
