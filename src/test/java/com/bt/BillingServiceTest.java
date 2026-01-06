package com.bt;

import com.bt.service.BillingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BillingServiceTest {
    private BillingService billingService;
    private final String TEST_USER = "testuser";
    private final String TEST_USER2 = "testuser2";
    private final String TEST_USER3 = "testuser3";

    @BeforeEach
    void setUp() {
        billingService = new BillingService();
    }

    private double getBillForDate(String user, LocalDate date) {
        Map<LocalDate, Double> summary = billingService.getBillingSummary(user);
        return summary.getOrDefault(date, 0.0);
    }

    @Test
    void testSingleDaySession() {
        LocalDateTime loginTime = LocalDateTime.of(2024, Month.JANUARY, 1, 10, 0);
        LocalDateTime logoutTime = loginTime.plusHours(2); // 2 hours session

        billingService.recordLogin(TEST_USER, loginTime);
        billingService.recordLogout(TEST_USER, logoutTime);

        double bill = getBillForDate(TEST_USER, LocalDate.of(2024, Month.JANUARY, 1));
        assertEquals(240.0, bill, 0.01, "Should charge flat daily rate of 240.0");
    }

    @Test
    void testOvernightSession() {
        LocalDateTime loginTime = LocalDateTime.of(2024, Month.JANUARY, 1, 23, 0);
        LocalDateTime logoutTime = LocalDateTime.of(2024, Month.JANUARY, 2, 2, 0);

        billingService.recordLogin(TEST_USER, loginTime);
        billingService.recordLogout(TEST_USER, logoutTime);

        double day1Bill = getBillForDate(TEST_USER, LocalDate.of(2024, Month.JANUARY, 1));
        assertEquals(240.0, day1Bill, 0.01, "Day 1 should be charged full daily rate");

        double day2Bill = getBillForDate(TEST_USER, LocalDate.of(2024, Month.JANUARY, 2));
        assertEquals(240.0, day2Bill, 0.01, "Day 2 should be charged full daily rate");
    }

    @Test
    void testMultipleSessionsSameDay() {
        LocalDateTime login1 = LocalDateTime.of(2024, Month.JANUARY, 1, 9, 0);
        LocalDateTime logout1 = login1.plusHours(2);

        LocalDateTime login2 = LocalDateTime.of(2024, Month.JANUARY, 1, 14, 0);
        LocalDateTime logout2 = login2.plusHours(3);

        billingService.recordLogin(TEST_USER, login1);
        billingService.recordLogout(TEST_USER, logout1);

        assertEquals(240.0, getBillForDate(TEST_USER, LocalDate.of(2024, Month.JANUARY, 1)), 0.01);

        billingService.recordLogin(TEST_USER, login2);
        billingService.recordLogout(TEST_USER, logout2);

        double totalBill = getBillForDate(TEST_USER, LocalDate.of(2024, Month.JANUARY, 1));
        assertEquals(240.0, totalBill, 0.01, "Multiple sessions in one day should only charge flat rate once");

        double nextDayBill = getBillForDate(TEST_USER, LocalDate.of(2024, Month.JANUARY, 2));
        assertEquals(0.0, nextDayBill, 0.01, "Next day should have no charge");
    }

    @Test
    void testBillingSummary() {
        LocalDateTime login1 = LocalDateTime.of(2024, Month.JANUARY, 1, 23, 0);
        LocalDateTime logout1 = LocalDateTime.of(2024, Month.JANUARY, 2, 1, 0);

        LocalDateTime login2 = LocalDateTime.of(2024, Month.JANUARY, 2, 12, 0);
        LocalDateTime logout2 = login2.plusHours(2);

        billingService.recordLogin(TEST_USER, login1);
        billingService.recordLogout(TEST_USER, logout1);
        billingService.recordLogin(TEST_USER, login2);
        billingService.recordLogout(TEST_USER, logout2);

        Map<LocalDate, Double> summary = billingService.getBillingSummary(TEST_USER);

        assertEquals(2, summary.size());
        assertEquals(240.0, summary.get(LocalDate.of(2024, Month.JANUARY, 1)), 0.01);
        assertEquals(240.0, summary.get(LocalDate.of(2024, Month.JANUARY, 2)), 0.01);
    }

    @Test
    void testActiveSession() {

        LocalDateTime loginTime = LocalDateTime.now().minusHours(1);
        billingService.recordLogin(TEST_USER, loginTime);

        double bill = getBillForDate(TEST_USER, LocalDate.now());
        assertEquals(240.0, bill, 0.01, "Active session should incur daily charge");
    }

    @Test
    void testMultipleLoginsWithoutLogout() {

        LocalDateTime login1 = LocalDateTime.of(2024, Month.JANUARY, 1, 9, 0);
        LocalDateTime login2 = LocalDateTime.of(2024, Month.JANUARY, 1, 14, 0);

        billingService.recordLogin(TEST_USER, login1);
        billingService.recordLogin(TEST_USER, login2); // Logic should handle the previous active session

        double bill = getBillForDate(TEST_USER, LocalDate.of(2024, Month.JANUARY, 1));
        assertEquals(240.0, bill, 0.01, "Should charge flat rate regardless of overlapping/reset sessions");
    }

    @Test
    void testAutoLogoutAfterInactivity() {


        LocalDateTime oldLoginTime = LocalDateTime.now().minusHours(25);
        billingService.recordLogin(TEST_USER, oldLoginTime);


        LocalDateTime newLoginTime = LocalDateTime.now();
        billingService.recordLogin(TEST_USER, newLoginTime);

        double billToday = getBillForDate(TEST_USER, LocalDate.now());
        assertEquals(240.0, billToday, 0.01, "Should charge flat rate for today");

        double billYesterday = getBillForDate(TEST_USER, LocalDate.now().minusDays(1));
        assertEquals(240.0, billYesterday, 0.01, "Should charge flat rate for yesterday");
    }

    @Test
    void testMultipleUsersWithMissingLogouts() {

        billingService.recordLogin(TEST_USER, LocalDateTime.of(2024, Month.JANUARY, 1, 9, 0));


        LocalDateTime login2 = LocalDateTime.of(2024, Month.JANUARY, 1, 10, 0);
        LocalDateTime logout2 = login2.plusHours(2);
        billingService.recordLogin(TEST_USER2, login2);
        billingService.recordLogout(TEST_USER2, logout2);


        billingService.recordLogin(TEST_USER3, LocalDateTime.of(2024, Month.JANUARY, 1, 23, 0));

        assertEquals(240.0, getBillForDate(TEST_USER, LocalDate.of(2024, Month.JANUARY, 1)), 0.01);
        assertEquals(240.0, getBillForDate(TEST_USER2, LocalDate.of(2024, Month.JANUARY, 1)), 0.01);
        assertEquals(240.0, getBillForDate(TEST_USER3, LocalDate.of(2024, Month.JANUARY, 1)), 0.01);
    }

    @Test
    void testSessionSpanningMultipleDaysWithoutLogout() {


        LocalDateTime loginTime = LocalDateTime.of(2024, Month.JANUARY, 1, 23, 0);
        billingService.recordLogin(TEST_USER, loginTime);

        double day1Bill = getBillForDate(TEST_USER, LocalDate.of(2024, Month.JANUARY, 1));
        assertEquals(240.0, day1Bill, 0.01);


    }
}