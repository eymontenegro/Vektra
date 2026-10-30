package com.vektra.model;

import java.time.Year;

/**
 * Vehículo del metro. Tiene su placa en un campo propio, la línea en la que
 * circula y el conductor asignado.
 *
 * El constructor solo valida lo mínimo: capacidad y año. Las demás reglas de
 * negocio (placa única, conductor existente) las comprueba VehicleService.
 */
public class Vehicle {

    /** Primer año que se acepta para un vehículo. */
    private static final int MIN_YEAR = 1900;

    /** Identificador del vehículo en la base de datos (0 si aún no se guarda). */
    private int id;

    /** Placa del vehículo, única en el sistema. */
    private final String plate;

    /** Marca del vehículo. */
    private final String brand;

    /** Modelo del vehículo. */
    private final String model;

    /** Año de fabricación. */
    private final int year;

    /** Número de pasajeros que admite, mayor que cero. */
    private final int capacity;

    /** Línea en la que circula el vehículo. */
    private final LineColor line;

    /** Conductor asignado al vehículo. */
    private final Driver driver;

    /**
     * Crea un vehículo con todos sus datos.
     *
     * @param id       identificador en la base de datos (0 si aún no se guarda)
     * @param plate    placa del vehículo
     * @param brand    marca
     * @param model    modelo
     * @param year     año de fabricación, entre 1900 y el año siguiente al actual
     * @param capacity capacidad de pasajeros, mayor que cero
     * @param line     línea en la que circula, obligatoria
     * @param driver   conductor asignado, obligatorio
     * @throws IllegalArgumentException si algún dato no es válido
     */
    public Vehicle(int id, String plate, String brand, String model, int year,
            int capacity, LineColor line, Driver driver) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("La capacidad del vehículo debe ser mayor que cero.");
        }
        if (year < MIN_YEAR || year > Year.now().getValue() + 1) {
            throw new IllegalArgumentException("El año del vehículo no es válido.");
        }
        if (line == null) {
            throw new IllegalArgumentException("El vehículo debe tener una línea.");
        }
        if (driver == null) {
            throw new IllegalArgumentException("El vehículo debe tener un conductor.");
        }
        this.id = id;
        this.plate = plate;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.capacity = capacity;
        this.line = line;
        this.driver = driver;
    }

    public int getId() {
        return id;
    }

    /** Lo usa el DAO para guardar el id generado por la base de datos. */
    public void setId(int id) {
        this.id = id;
    }

    public String getPlate() {
        return plate;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public int getCapacity() {
        return capacity;
    }

    public LineColor getLine() {
        return line;
    }

    public Driver getDriver() {
        return driver;
    }

    /**
     * Nombre completo del conductor, para no encadenar
     * vehicle.getDriver().getFullName() en las vistas.
     *
     * @return nombre y apellido del conductor
     */
    public String getDriverName() {
        return driver.getFullName();
    }

    /**
     * Nombre de la línea en español, por ejemplo "Roja".
     *
     * @return nombre de la línea que ve el usuario
     */
    public String getLineName() {
        return line.getDisplayName();
    }

    @Override
    public String toString() {
        return "Vehicle{id=" + id + ", plate=" + plate + ", line=" + line + "}";
    }
}