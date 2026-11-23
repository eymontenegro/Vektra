package com.vektra.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pruebas del cálculo de porcentajes del dashboard.
 */
class DashboardControllerTest {

    @Test
    void percentRoundsToNearestInteger() {
        assertEquals(33, DashboardController.percent(1, 3));
        assertEquals(67, DashboardController.percent(2, 3));
    }

    @Test
    void percentOfZeroTotalIsZero() { // Sin tickets vendidos no se divide entre cero. 
        assertEquals(0, DashboardController.percent(0, 0));
    }

    @Test
    void wholeTotalIsOneHundredPercent() {
        assertEquals(100, DashboardController.percent(12, 12));
    }
}
