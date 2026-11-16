package com.trackflow.modules.pickups.infrastructure;

import com.trackflow.modules.pickups.application.CodigosDeGestion;
import java.security.SecureRandom;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CodigosDeGestionBCrypt implements CodigosDeGestion {
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();
    private final PasswordEncoder encoder;
    private final String hashDeRelleno;

    public CodigosDeGestionBCrypt(PasswordEncoder encoder) {
        this.encoder = encoder;
        this.hashDeRelleno = encoder.encode("CODIGO-DE-RELLENO");
    }

    @Override
    public String generar() {
        StringBuilder codigo = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            codigo.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        }
        return codigo.toString();
    }

    @Override
    public String hash(String codigo) {
        return encoder.encode(codigo);
    }

    @Override
    public boolean coincide(String codigo, String hash) {
        boolean formatoValido = codigo != null && codigo.matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}");
        String candidato = formatoValido ? codigo : "INVALIDO";
        boolean coincide = encoder.matches(candidato, hash == null || hash.isBlank() ? hashDeRelleno : hash);
        return formatoValido && hash != null && !hash.isBlank() && coincide;
    }
}
