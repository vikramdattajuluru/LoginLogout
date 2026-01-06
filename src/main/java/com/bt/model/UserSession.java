package com.bt.model;

import java.time.LocalDateTime;

public class UserSession {
    private final String username;
    private final LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private boolean active;
    private boolean autoLoggedOut;

    public UserSession(String username, LocalDateTime loginTime) {
        this.username = username;
        this.loginTime = loginTime;
        this.active = true;
        this.autoLoggedOut = false;
    }

    public void logout(LocalDateTime logoutTime) {
        if (this.active) {
            this.logoutTime = logoutTime;
            this.active = false;
        }
    }

    public void autoLogout(LocalDateTime logoutTime) {
        if (this.active) {
            this.logoutTime = logoutTime;
            this.active = false;
            this.autoLoggedOut = true;
        }
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public boolean isActive() {
        return active;
    }

    public boolean wasAutoLoggedOut() {
        return autoLoggedOut;
    }

    public long getSessionDurationInMinutes() {
        if (loginTime == null) {
            return -1; // Invalid session
        }

        LocalDateTime endTime = active ? LocalDateTime.now() :
                (logoutTime != null ? logoutTime : LocalDateTime.now());

        return java.time.Duration.between(loginTime, endTime).toMinutes();
    }
}
