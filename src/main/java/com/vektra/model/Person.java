package com.vektra.model;

/**
 * Persona del sistema. Es abstracta: cada subclase (pasajero, conductor)
 * declara su propio {@link Role}. La cédula es un texto aparte; el id es
 * un entero generado por la base de datos.
 */
public abstract class Person {

    private int id;
    private final String nationalId;
    private final String name;
    private final String lastName;

    /**
     * @param id         id en la base de datos (0 si aún no se guarda)
     * @param nationalId cédula, como texto
     * @param name       nombres
     * @param lastName   apellidos
     */
    protected Person(int id, String nationalId, String name, String lastName) {
        this.id = id;
        this.nationalId = nationalId;
        this.name = name;
        this.lastName = lastName;
    }

    /** @return el rol de esta persona */
    public abstract Role getRole();

    public int getId() {
        return id;
    }

    /** Lo usa el DAO para guardar el id generado por la base de datos. */
    public void setId(int id) {
        this.id = id;
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    /** @return nombre y apellido juntos */
    public String getFullName() {
        return name + " " + lastName;
    }
}
