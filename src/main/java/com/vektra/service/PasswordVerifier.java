package com.vektra.service;

/** Verifica una contraseña en texto plano contra su hash. */
public interface PasswordVerifier {
    boolean matches(String plainPassword, String hash);
}