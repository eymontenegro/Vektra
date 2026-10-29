package com.vektra.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pasajero registrado. El atributo {@code password} guarda el hash BCrypt,
 * nunca la contraseña en texto plano.
 */
public class Passenger extends Person {

    private final String email;
    private final String password;
    private final LocalDate birthDate;
    private final LocalDateTime registrationDate;

    /**
     * @param id               id del pasajero en la base de datos (0 si aún no se guarda)
     * @param nationalId       número de documento de identidad
     * @param name             nombres del pasajero
     * @param lastName         apellidos del pasajero
     * @param email            correo electrónico del pasajero
     * @param password         hash BCrypt de la contraseña
     * @param birthDate        fecha de nacimiento (puede ser nula)
     * @param registrationDate fecha de registro (puede ser nula si la base no la tiene)
     */
    public Passenger(int id, String nationalId, String name, String lastName,
                     String email, String password,
                     LocalDate birthDate, LocalDateTime registrationDate) {
        super(id, nationalId, name, lastName);
        this.email = email;
        this.password = password;
        this.birthDate = birthDate;
        this.registrationDate = registrationDate;
    }

    @Override
    public Role getRole() {
        return Role.PASSENGER;
    }

    public String getEmail() {
        return email;
    }

    
    /** @return el hash BCrypt de la contraseña */
    public String getPasswordHash() {
        return password;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    /** No incluye la contraseña. */
    @Override
    public String toString() {
        return "Passenger{id=" + getId() + ", name=" + getFullName() + ", email=" + email + "}";
    }
}