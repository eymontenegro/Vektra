package com.vektra.model;

import java.awt.Color;

/**
 * Líneas del metro de Vektra, identificadas por su color.
 *
 * Reemplaza los textos sueltos que antes se usaban para el color de la línea.
 * En la base de datos se guarda el nombre de la constante (por ejemplo RED),
 * y cada valor sabe el nombre que ve el usuario y el color con el que se
 * dibuja en el mapa.
 */
public enum LineColor {

    RED("Roja", "#E50822"),
    BLUE("Azul", "#2980B9"),
    GREEN("Verde", "#27AE60"),
    YELLOW("Amarilla", "#F39C12");

    /** Nombre de la línea que ve el usuario. */
    private final String displayName;

    /** Color de la línea en formato hexadecimal, usado por el mapa. */
    private final String hexCode;

    LineColor(String displayName, String hexCode) {
        this.displayName = displayName;
        this.hexCode = hexCode;
    }

    /**
     * Nombre de la línea en español, por ejemplo "Roja".
     *
     * @return nombre que ve el usuario
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Color de la línea, por ejemplo "#E50822".
     *
     * @return código hexadecimal del color
     */
    public String getHexCode() {
        return hexCode;
    }

    /**
     * Color de la línea listo para pintar en Swing. Así ninguna vista
     * tiene que escribir Color.decode (regla 21).
     *
     * @return color de la línea
     */
    public Color toColor() {
        return Color.decode(hexCode);
    }

    /**
     * Convierte el valor guardado en la base de datos en una línea.
     *
     * @param code valor de la columna line_color, por ejemplo "RED"
     * @return línea correspondiente
     * @throws IllegalArgumentException si el valor no corresponde a ninguna línea
     */
    public static LineColor fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("La línea no tiene color asignado.");
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La línea '" + code + "' no existe.", e);
        }
    }
}