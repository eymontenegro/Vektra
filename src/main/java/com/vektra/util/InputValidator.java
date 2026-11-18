package com.vektra.util;
import com.vektra.exception.ValidationException;
import java.util.regex.Pattern;
/**
 * Validaciones de texto que comparten todos los servicios.
 *
 * Antes requireText y blankToNull estaban copiados en DriverService y en
 * VehicleService; ahora viven en un solo lugar. Cada método lanza
 * ValidationException con el mensaje que se le pase, en español.
 */
public final class InputValidator {
    /** Solo dígitos, entre 6 y 12: cédula (igual que la base de datos). */
    public static final Pattern NATIONAL_ID = Pattern.compile("\\d{6,12}");
    /** Solo dígitos, entre 7 y 15: teléfono (igual que la base de datos). */
    public static final Pattern PHONE = Pattern.compile("\\d{7,15}");
    /** Formato básico de correo: algo@algo.algo */
    public static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    /** Constructor privado: esta clase solo tiene métodos estáticos. */
    private InputValidator() {
    }
    /**
     * Quita los espacios de los extremos.
     *
     * @param value texto recibido; puede ser null
     * @return el texto limpio, o cadena vacía si era null
     */
    public static String clean(String value) {
        return value == null ? "" : value.trim();
    }
    /**
     * Exige un texto no vacío.
     *
     * @param value   texto recibido
     * @param message mensaje si está vacío
     * @return el texto sin espacios en los extremos
     * @throws ValidationException si es null o está vacío
     */
    public static String requireText(String value, String message) {
        String cleanValue = clean(value);
        if (cleanValue.isEmpty()) {
            throw new ValidationException(message);
        }
        return cleanValue;
    }
    /**
     * Limpia un texto opcional.
     *
     * @param value texto recibido
     * @return el texto sin espacios, o null si venía vacío
     */
    public static String optionalText(String value) {
        String cleanValue = clean(value);
        return cleanValue.isEmpty() ? null : cleanValue;
    }
    /**
     * Exige que un texto no pase de cierto largo (el de su columna en la base).
     *
     * @param value     texto ya limpio; null se acepta
     * @param maxLength largo máximo
     * @param message   mensaje si es más largo
     * @return el mismo texto
     * @throws ValidationException si es más largo que maxLength
     */
    public static String requireMaxLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new ValidationException(message);
        }
        return value;
    }
    /**
     * Exige que un texto cumpla un patrón.
     *
     * @param value   texto ya limpio; null se acepta (para campos opcionales)
     * @param pattern patrón que debe cumplir
     * @param message mensaje si no lo cumple
     * @return el mismo texto
     * @throws ValidationException si no cumple el patrón
     */
    public static String requirePattern(String value, Pattern pattern, String message) {
        if (value != null && !pattern.matcher(value).matches()) {
            throw new ValidationException(message);
        }
        return value;
    }
    /**
     * Convierte un texto en un número entero.
     *
     * @param value   texto recibido, por ejemplo de un formulario
     * @param message mensaje si no es un número
     * @return el número
     * @throws ValidationException si está vacío o no es un número
     */
    public static int requireInt(String value, String message) {
        try {
            return Integer.parseInt(requireText(value, message));
        } catch (NumberFormatException e) {
            throw new ValidationException(message, e);
        }
    }
}