package com.vektra.web.controller;

import com.vektra.exception.DataAccessException;
import com.vektra.exception.ValidationException;
import com.vektra.model.Passenger;
import com.vektra.model.User;
import com.vektra.security.LoginAttemptLimiter;
import com.vektra.security.SessionUser;
import com.vektra.service.PassengerService;
import com.vektra.service.UserService;
import com.vektra.web.Controller;
import com.vektra.web.FlashMessages;
import com.vektra.web.TemplateRenderer;
import com.vektra.web.WebSession;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Inicio de sesión, registro de pasajeros y cierre de sesión.
 * Reemplaza a LoginFrame y CreateAccountFrame.
 *
 * Rutas:
 *   GET  /          redirige al mapa
 *   GET  /login     formulario de inicio de sesión
 *   POST /login     valida las credenciales
 *   GET  /register  formulario de crear cuenta
 *   POST /register  registra al pasajero
 *   POST /logout    cierra la sesión
 */
public class AuthController implements Controller {

    /** Código HTTP para un formulario con datos inválidos que se vuelve a mostrar. */
    private static final int UNPROCESSABLE = 422;

    /** Valor del selector "Tipo de usuario" para el administrador. */
    private static final String ADMIN_OPTION = "ADMIN";

    private final UserService userService;
    private final PassengerService passengerService;
    private final LoginAttemptLimiter loginLimiter;
    private final TemplateRenderer renderer;

    /**
     * @param userService      autenticación del administrador
     * @param passengerService registro y autenticación de pasajeros
     * @param loginLimiter     límite de intentos fallidos de inicio de sesión
     * @param renderer         motor de plantillas
     */
    public AuthController(UserService userService, PassengerService passengerService,
                          LoginAttemptLimiter loginLimiter, TemplateRenderer renderer) {
        this.userService = userService;
        this.passengerService = passengerService;
        this.loginLimiter = loginLimiter;
        this.renderer = renderer;
    }

    @Override
    public void register(Javalin app) {
        app.get("/", ctx -> ctx.redirect("/map", HttpStatus.FOUND));
        app.get("/login", this::showLogin);
        app.post("/login", this::login);
        app.get("/register", this::showRegister);
        app.post("/register", this::registerPassenger);
        app.post("/logout", this::logout);
    }

    private void showLogin(Context ctx) {
        if (WebSession.currentUser(ctx) != null) {
            ctx.redirect("/map", HttpStatus.FOUND);
            return;
        }
        renderer.render(ctx, "auth/login", Map.of("userType", "PASSENGER", "email", ""));
    }

    private void login(Context ctx) {
        String userType = ADMIN_OPTION.equals(ctx.formParam("userType")) ? ADMIN_OPTION : "PASSENGER";
        String email = ctx.formParam("email");
        String password = ctx.formParam("password");
        Duration lock = loginLimiter.remainingLock(email);
        if (!lock.isZero()) {
            long minutes = Math.max(1, (lock.toSeconds() + 59) / 60);
            showLoginError(ctx, userType, email, "Demasiados intentos fallidos. Espera " + minutes
                    + (minutes == 1 ? " minuto" : " minutos") + " e intenta de nuevo.");
            return;
        }
        try {
            SessionUser user;
            if (ADMIN_OPTION.equals(userType)) {
                User admin = userService.authenticate(email, password);
                user = SessionUser.fromAdmin(admin);
            } else {
                Passenger passenger = passengerService.authenticate(email, password);
                user = SessionUser.fromPassenger(passenger);
            }
            loginLimiter.recordSuccess(email);
            WebSession.login(ctx, user);
            FlashMessages.success(ctx, "¡Hola, " + user.name() + "!");
            ctx.redirect("/map", HttpStatus.FOUND);
        } catch (ValidationException e) {
            // Credenciales incorrectas o campos vacíos: cuenta como intento fallido.
            loginLimiter.recordFailure(email);
            showLoginError(ctx, userType, email, e.getMessage());
        } catch (DataAccessException e) {
            // Un fallo de la base de datos no es culpa de quien intenta entrar: no cuenta.
            showLoginError(ctx, userType, email, e.getMessage());
        }
    }

    /** Vuelve a mostrar el login con el error. La contraseña nunca se devuelve al formulario. */
    private void showLoginError(Context ctx, String userType, String email, String error) {
        ctx.status(UNPROCESSABLE);
        renderer.render(ctx, "auth/login", Map.of(
                "userType", userType,
                "email", email == null ? "" : email,
                "error", error));
    }

    private void showRegister(Context ctx) {
        renderer.render(ctx, "auth/register", Map.of(
                "form", Map.of(),
                "minPasswordLength", PassengerService.MIN_PASSWORD_LENGTH,
                "today", LocalDate.now().toString()));
    }

    private void registerPassenger(Context ctx) {
        Map<String, String> form = new HashMap<>();
        for (String field : new String[]{"nationalId", "name", "lastName", "email", "birthDate"}) {
            String value = ctx.formParam(field);
            form.put(field, value == null ? "" : value);
        }
        try {
            passengerService.register(form.get("nationalId"), form.get("name"), form.get("lastName"),
                    form.get("email"), ctx.formParam("password"), parseDate(form.get("birthDate")));
            FlashMessages.success(ctx, "¡Cuenta creada con éxito! Ya puedes iniciar sesión.");
            ctx.redirect("/login", HttpStatus.FOUND);
        } catch (ValidationException | DataAccessException e) {
            ctx.status(UNPROCESSABLE);
            renderer.render(ctx, "auth/register", Map.of(
                    "form", form,
                    "minPasswordLength", PassengerService.MIN_PASSWORD_LENGTH,
                    "today", LocalDate.now().toString(),
                    "error", e.getMessage()));
        }
    }

    private void logout(Context ctx) {
        WebSession.logout(ctx);
        FlashMessages.success(ctx, "Cerraste sesión. ¡Hasta pronto!");
        ctx.redirect("/login", HttpStatus.FOUND);
    }

    /**
     * Convierte la fecha del campo type="date" (aaaa-mm-dd).
     *
     * @param value texto del formulario
     * @return la fecha, o null si viene vacía (el servicio la exige)
     * @throws ValidationException si no es una fecha válida
     */
    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("La fecha de nacimiento no es válida.", e);
        }
    }
}
