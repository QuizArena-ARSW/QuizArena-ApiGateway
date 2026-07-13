package com.quizarena.gateway.filtro;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Filtro global que valida el token JWT ANTES de enrutar la peticion.
 *
 * - Las rutas publicas (login, registro, websocket) pasan sin validacion.
 * - Las peticiones OPTIONS (preflight CORS) pasan sin validacion.
 * - El resto debe traer un token valido en 'Authorization: Bearer ...'.
 *
 * Asi la autenticacion se centraliza en la entrada del sistema, en vez de
 * repetirse en cada microservicio.
 */
@Component
public class FiltroAutenticacionJwt implements GlobalFilter, Ordered {

    private final SecretKey clave;
    private final List<String> rutasPublicas;

    public FiltroAutenticacionJwt(
            @Value("${quizarena.jwt.secret}") String secret,
            @Value("${quizarena.seguridad.rutas-publicas}") List<String> rutasPublicas) {
        this.clave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.rutasPublicas = rutasPublicas;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String ruta = request.getURI().getPath();

        // Dejar pasar preflight CORS
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // Dejar pasar rutas publicas (login, registro, websocket)
        if (esRutaPublica(ruta)) {
            return chain.filter(exchange);
        }

        // Extraer el token de la cabecera Authorization
        String cabecera = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            return rechazar(exchange, "Falta el token de autenticacion");
        }

        String token = cabecera.substring(7);
        try {
            // Si el token es invalido o expiro, esto lanza excepcion
            Jwts.parser().verifyWith(clave).build().parseSignedClaims(token);
        } catch (Exception e) {
            return rechazar(exchange, "Token invalido o expirado");
        }

        // Token valido: dejar continuar hacia el microservicio
        return chain.filter(exchange);
    }

    private boolean esRutaPublica(String ruta) {
        return rutasPublicas.stream().anyMatch(ruta::startsWith);
    }

    private Mono<Void> rechazar(ServerWebExchange exchange, String mensaje) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Motivo", mensaje);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // se ejecuta temprano, antes de enrutar
    }
}
