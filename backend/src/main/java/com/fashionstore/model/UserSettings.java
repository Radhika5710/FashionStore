package com.fashionstore.model;

import java.sql.Timestamp;

public class UserSettings {
    private int settingId;
    private int userId;
    private boolean emailNotifications;
    private boolean smsNotifications;
    private boolean orderUpdates;
    private boolean promotionalEmails;
    private String language;
    private String currency;
    private String themePreference;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructors
    public UserSettings() {}

    public UserSettings(int userId) {
        this.userId = userId;
        this.emailNotifications = true;
        this.smsNotifications = false;
        this.orderUpdates = true;
        this.promotionalEmails = false;
        this.language = "en";
        this.currency = "INR";
        this.themePreference = "auto";
    }

    // Getters and Setters
    public int getSettingId() { return settingId; }
    public void setSettingId(int settingId) { this.settingId = settingId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }

    public boolean isSmsNotifications() { return smsNotifications; }
    public void setSmsNotifications(boolean smsNotifications) { this.smsNotifications = smsNotifications; }

    public boolean isOrderUpdates() { return orderUpdates; }
    public void setOrderUpdates(boolean orderUpdates) { this.orderUpdates = orderUpdates; }

    public boolean isPromotionalEmails() { return promotionalEmails; }
    public void setPromotionalEmails(boolean promotionalEmails) { this.promotionalEmails = promotionalEmails; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getThemePreference() { return themePreference; }
    public void setThemePreference(String themePreference) { this.themePreference = themePreference; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "UserSettings{" +
                "settingId=" + settingId +
                ", userId=" + userId +
                ", emailNotifications=" + emailNotifications +
                ", smsNotifications=" + smsNotifications +
                ", orderUpdates=" + orderUpdates +
                ", promotionalEmails=" + promotionalEmails +
                ", language='" + language + '\'' +
                ", currency='" + currency + '\'' +
                ", themePreference='" + themePreference + '\'' +
                '}';
    }
}
