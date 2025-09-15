package io.lokal.notifications;

/**
 * User preferences object to pass language and notification settings
 * instead of using SharedPreferences
 */
public class UserPreferences {
    private String selectedLanguage;
    private String preferredLocale;
    private boolean isNotificationGroupingActive;
    private boolean keepNotificationAtTop;
    private boolean isSilentPush;
    
    public UserPreferences() {
        // Default values
        this.selectedLanguage = "en";
        this.preferredLocale = "none";
        this.isNotificationGroupingActive = true;
        this.keepNotificationAtTop = false;
        this.isSilentPush = false;
    }
    
    public UserPreferences(String selectedLanguage, String preferredLocale, 
                          boolean isNotificationGroupingActive, boolean keepNotificationAtTop, 
                          boolean isSilentPush) {
        this.selectedLanguage = selectedLanguage != null ? selectedLanguage : "en";
        this.preferredLocale = preferredLocale != null ? preferredLocale : "none";
        this.isNotificationGroupingActive = isNotificationGroupingActive;
        this.keepNotificationAtTop = keepNotificationAtTop;
        this.isSilentPush = isSilentPush;
    }
    
    // Getters
    public String getSelectedLanguage() {
        return selectedLanguage;
    }
    
    public String getPreferredLocale() {
        return preferredLocale;
    }
    
    public boolean isNotificationGroupingActive() {
        return isNotificationGroupingActive;
    }
    
    public boolean isKeepNotificationAtTop() {
        return keepNotificationAtTop;
    }
    
    public boolean isSilentPush() {
        return isSilentPush;
    }
    
    // Setters
    public void setSelectedLanguage(String selectedLanguage) {
        this.selectedLanguage = selectedLanguage != null ? selectedLanguage : "en";
    }
    
    public void setPreferredLocale(String preferredLocale) {
        this.preferredLocale = preferredLocale != null ? preferredLocale : "none";
    }
    
    public void setNotificationGroupingActive(boolean notificationGroupingActive) {
        this.isNotificationGroupingActive = notificationGroupingActive;
    }
    
    public void setKeepNotificationAtTop(boolean keepNotificationAtTop) {
        this.keepNotificationAtTop = keepNotificationAtTop;
    }
    
    public void setSilentPush(boolean silentPush) {
        this.isSilentPush = silentPush;
    }
    
    /**
     * Get the effective language locale following the same logic as native LanguageUtils
     * @param skipPreferredLocale if true, returns selectedLanguage; if false, returns preferredLocale or selectedLanguage
     * @return the language locale to use
     */
    public String getSelectedLanguageLocale(boolean skipPreferredLocale) {
        if (skipPreferredLocale) {
            return selectedLanguage;
        }
        
        if (preferredLocale.equalsIgnoreCase("none")) {
            return selectedLanguage;
        } else {
            return preferredLocale;
        }
    }
}
