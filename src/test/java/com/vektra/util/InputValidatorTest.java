package com.vektra.util;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.vektra.exception.ValidationException;
import org.junit.jupiter.api.Test;
/** Pruebas de las validaciones de texto compartidas por los servicios. */
class InputValidatorTest {
@Test
void requireTextTrimsValue() {
assertEquals("Ana", InputValidator.requireText("  Ana  ", "obligatorio"));
}
@Test
void requireTextRejectsBlank() {
ValidationException error = assertThrows(ValidationException.class,
() -> InputValidator.requireText("   ", "El nombre es obligatorio."));
assertEquals("El nombre es obligatorio.", error.getMessage());
}
@Test
void requireTextRejectsNull() {
assertThrows(ValidationException.class, () -> InputValidator.requireText(null, "x"));
}
@Test
void optionalTextTurnsBlankIntoNull() {
assertNull(InputValidator.optionalText("  "));
assertEquals("LIC-1", InputValidator.optionalText(" LIC-1 "));
}
@Test
    void nationalIdPatternAcceptsOnlyDigits() {
        assertEquals("1234567", InputValidator.requirePattern("1234567", InputValidator.NATIONAL_ID, "x"));
        assertThrows(ValidationException.class,
                () -> InputValidator.requirePattern("12.345.678", InputValidator.NATIONAL_ID, "x"));
        assertThrows(ValidationException.class,
                () -> InputValidator.requirePattern("12345", InputValidator.NATIONAL_ID, "x"));
    }
    @Test
    void patternIgnoresNullForOptionalFields() {
        assertNull(InputValidator.requirePattern(null, InputValidator.PHONE, "x"));
    }
    @Test
    void maxLengthRejectsLongText() {
        assertThrows(ValidationException.class, () -> InputValidator.requireMaxLength("ABCDEF", 5, "x"));
        assertEquals("ABCDE", InputValidator.requireMaxLength("ABCDE", 5, "x"));
    }
    @Test
    void requireIntParsesNumbers() {
        assertEquals(42, InputValidator.requireInt(" 42 ", "x"));
        assertThrows(ValidationException.class, () -> InputValidator.requireInt("cuatro", "x"));
        assertThrows(ValidationException.class, () -> InputValidator.requireInt("", "x"));
    }
}