package com.vektra.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Ticket comprado por un pasajero entre dos estaciones.
 * Ofrece métodos como {@link #getPassengerEmail()} para que quien lo use
 * no encadene llamadas (Ley de Demeter).
 */
public class Ticket {

    private int id;
    private final String code;
    private final LocalDateTime purchaseDate;
    private final BigDecimal price;
    private final Passenger passenger;
    private final Station origin;
    private final Station destination;

    /**
     * @param id           id en la base de datos (0 si aún no se guarda)
     * @param code         código del ticket
     * @param purchaseDate fecha y hora de compra
     * @param price        precio del ticket
     * @param passenger    pasajero que compró el ticket (no puede ser nulo)
     * @param origin       estación de origen (no puede ser nula)
     * @param destination  estación de destino (no puede ser nula)
     * @throws NullPointerException si passenger, origin o destination son nulos
     */
    public Ticket(int id, String code, LocalDateTime purchaseDate, BigDecimal price,
                  Passenger passenger, Station origin, Station destination) {
        this.id = id;
        this.code = code;
        this.purchaseDate = purchaseDate;
        this.price = price;
        this.passenger = Objects.requireNonNull(passenger, "passenger no puede ser nulo");
        this.origin = Objects.requireNonNull(origin, "origin no puede ser nula");
        this.destination = Objects.requireNonNull(destination, "destination no puede ser nula");
    }

    public int getId() {
        return id;
    }

    /** Lo usa el DAO para guardar el id generado por la base de datos. */
    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getPassengerId() {
        return passenger.getId();
    }

    public String getPassengerName() {
        return passenger.getFullName();
    }

    public String getPassengerEmail() {
        return passenger.getEmail();
    }

    public int getOriginId() {
        return origin.getId();
    }

    public String getOriginName() {
        return origin.getName();
    }

    public int getDestinationId() {
        return destination.getId();
    }

    public String getDestinationName() {
        return destination.getName();
    }
}