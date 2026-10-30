package com.vektra.model;

/**
 * Cuenta del administrador. Ya existe en la base de datos (la crea el script
 * SQL) y no se registra desde la aplicación.
 */
public class User {

    private final int id;
    private final String name;
    private final String email;
    private final String password;
    private final Role role;

    /**
     * @param password hash BCrypt de la contraseña
     */
    public User(int id, String name, String email, String password, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    /**
     * @return el hash BCrypt de la contraseña
     */
    public String getPasswordHash() {
        return password;
    }
    
    public Role getRole() {
        return role;
    }

    /** No incluye la contraseña. */
    @Override
    public String toString() {
        return "User{id=" + id + ", email=" + email + ", role=" + role + "}";
    }
}
