package com.bt.service;

import com.bt.model.UserSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class BillingService {
    private final Map<String, List<UserSession>> userSessions = new HashMap<>();
    private static final double DAILY_RATE = 240.0;
    private static final long MAX_SESSION_HOURS = 24;

    public void recordLogin(String username, LocalDateTime loginTime) {
        List<UserSession> sessions = userSessions.computeIfAbsent(username, k -> new ArrayList<>());

        sessions.stream()
                .filter(UserSession::isActive)
                .filter(s -> s.getLoginTime().plusHours(MAX_SESSION_HOURS).isBefore(loginTime))
                .forEach(s -> s.autoLogout(s.getLoginTime().plusHours(MAX_SESSION_HOURS)));

        sessions.stream()
                .filter(UserSession::isActive)
                .findFirst()
                .ifPresent(s -> s.autoLogout(loginTime));

        sessions.add(new UserSession(username, loginTime));
    }

    public void recordLogout(String username, LocalDateTime logoutTime) {
        List<UserSession> sessions = userSessions.get(username);
        if (sessions != null && !sessions.isEmpty()) {
            sessions.stream()
                    .filter(UserSession::isActive)
                    .findFirst()
                    .ifPresent(session -> session.logout(logoutTime));
        }
    }

    public Map<LocalDate, Double> getBillingSummary(String username) {
        List<UserSession> sessions = userSessions.getOrDefault(username, Collections.emptyList());
        Map<LocalDate, Double> dailyBills = new TreeMap<>(); // TreeMap to keep dates sorted

        for (UserSession session : sessions) {
            LocalDate currentDate = session.getLoginTime().toLocalDate();

            LocalDate endDate = session.isActive()
                    ? LocalDate.now()
                    : session.getLogoutTime().toLocalDate();

            for (LocalDate date = currentDate; !date.isAfter(endDate); date = date.plusDays(1)) {

                dailyBills.put(date, DAILY_RATE);
            }
        }

        return dailyBills;
    }
}