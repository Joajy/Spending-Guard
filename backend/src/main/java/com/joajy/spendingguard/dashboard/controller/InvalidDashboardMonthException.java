package com.joajy.spendingguard.dashboard.controller;

class InvalidDashboardMonthException extends RuntimeException {
    InvalidDashboardMonthException() {
        super("month must use YYYY-MM format");
    }
}
