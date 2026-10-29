package com.vektra.model;

/**
 * Rol de una cuenta o persona del sistema. Cada valor sabe qué puede hacer,
 * así las vistas y los servicios no comparan textos como "admin".
 */
public enum Role {
    ADMIN(true, false, true),
    PASSENGER(false, true, false),
    DRIVER(false, false, false);

    private final boolean manageFleet;
    private final boolean buyTickets;
    private final boolean viewDashboard;

    Role(boolean manageFleet, boolean buyTickets, boolean viewDashboard) {
        this.manageFleet = manageFleet;
        this.buyTickets = buyTickets;
        this.viewDashboard = viewDashboard;
    }

    /** @return true si puede añadir, editar o borrar conductores y vehículos */
    public boolean canManageFleet() {
        return manageFleet;
    }

    /** @return true si puede comprar tickets */
    public boolean canBuyTickets() {
        return buyTickets;
    }

    /** @return true si puede ver el dashboard de métricas */
    public boolean canViewDashboard() {
        return viewDashboard;
    }
}
