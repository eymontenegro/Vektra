package com.vektra.security;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
* Limita los intentos fallidos de inicio de sesión para frenar los ataques
* de fuerza bruta (probar miles de contraseñas).
*
* Después de {@link #MAX_FAILURES} intentos fallidos seguidos con el mismo
* correo, ese correo queda bloqueado durante {@link #LOCK_DURATION}. Un
* inicio de sesión correcto borra el contador. Los datos viven en memoria:
* se pierden al reiniciar el servidor, lo cual es suficiente para este proyecto.
*/
public class LoginAttemptLimiter {
/** Intentos fallidos permitidos antes del bloqueo. */
public static final int MAX_FAILURES = 5;
/** Tiempo que dura el bloqueo. */
public static final Duration LOCK_DURATION = Duration.ofMinutes(5);
/** Estado de un correo: fallos seguidos y hasta cuándo está bloqueado. */
private record Attempts(int failures, Instant lockedUntil) {
}
    private final Map<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();
    private final Clock clock;
    /** Crea el limitador con el reloj del sistema. */
    public LoginAttemptLimiter() {
        this(Clock.systemUTC());
    }
    /**
     * Crea el limitador con un reloj propio. Las pruebas lo usan para
     * "adelantar el tiempo" sin esperar 5 minutos de verdad.
     *
     * @param clock reloj a usar
     */
    public LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }
    /**
     * Indica cuánto falta para que termine el bloqueo.
     *
     * @param email correo con el que se intenta entrar
     * @return tiempo restante, o {@link Duration#ZERO} si no está bloqueado
     */
    public Duration remainingLock(String email) {
        Attempts attempts = attemptsByKey.get(key(email));
        if (attempts == null || attempts.lockedUntil() == null) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(clock.instant(), attempts.lockedUntil());
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
    /**
     * @param email correo con el que se intenta entrar
     * @return true si está bloqueado en este momento
     */
    public boolean isBlocked(String email) {
        return !remainingLock(email).isZero();
    }
    /**
     * Registra un intento fallido. Al llegar a {@link #MAX_FAILURES}, bloquea el correo.
     *
     * @param email correo con el que se intentó entrar
     */
    public void recordFailure(String email) {
        Instant now = clock.instant();
        attemptsByKey.compute(key(email), (k, current) -> {
            // Si un bloqueo anterior ya terminó, se empieza a contar de cero.
            boolean expired = current != null && current.lockedUntil() != null
                    && !now.isBefore(current.lockedUntil());
            int failures = (current == null || expired) ? 1 : current.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_DURATION) : null;
            return new Attempts(failures, lockedUntil);
        });
    }
    /**
     * Borra los fallos de un correo después de un inicio de sesión correcto.
     *
     * @param email correo que inició sesión
*/
public void recordSuccess(String email) {
attemptsByKey.remove(key(email));
}
/** Mismo correo con mayúsculas o espacios cuenta como el mismo. */
private static String key(String email) {
return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
}
}