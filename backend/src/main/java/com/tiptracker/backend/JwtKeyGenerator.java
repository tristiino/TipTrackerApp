package com.tiptracker.backend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;

public class JwtKeyGenerator {
    public static void main(String[] args) {
        byte[] key = new byte[64]; // 512-bit key
        new SecureRandom().nextBytes(key);
        String secret = Base64.getEncoder().encodeToString(key);
        System.out.println("Generated JWT Secret Key:");
        System.out.println(secret);

        System.out.println(new BCryptPasswordEncoder().encode("password123"));
    }
}
