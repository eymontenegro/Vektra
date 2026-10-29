package com.vektra.service;

import org.mindrot.jbcrypt.BCrypt;

/** Implementación de {@link PasswordVerifier} con BCrypt. */
public class BcryptPasswordVerifier implements PasswordVerifier {

    @Override
    public boolean matches(String plainPassword, String hash) {
        return BCrypt.checkpw(plainPassword, hash);
    }
}