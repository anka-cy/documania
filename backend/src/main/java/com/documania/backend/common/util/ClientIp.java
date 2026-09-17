package com.documania.backend.common.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIp {

    private ClientIp() {}

    /**
     * IP du client : derrière le proxy d'ingress, seule la valeur la plus à droite de
     * X-Forwarded-For est ajoutée par notre infrastructure ; les entrées de gauche sont
     * falsifiables par le client (contournement du limiteur de débit). Sinon, adresse socket.
     */
    public static String from(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.lastIndexOf(',');
            String last = comma >= 0 ? forwarded.substring(comma + 1) : forwarded;
            String ip = last.trim();
            if (!ip.isEmpty()) {
                return ip;
            }
        }
        return request.getRemoteAddr();
    }
}
