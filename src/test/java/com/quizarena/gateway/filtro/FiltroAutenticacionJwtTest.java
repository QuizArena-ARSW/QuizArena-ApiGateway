package com.quizarena.gateway.filtro;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El Gateway es el punto de entrada unico: este filtro es lo que decide, en
 * la entrada, si una peticion pasa hacia los microservicios o se rechaza.
 * Estas pruebas cubren rutas publicas, peticiones sin token, con token
 * invalido y con token valido — sin levantar ningun microservicio real.
 */
class FiltroAutenticacionJwtTest {

    private static final String SECRETO = "clave-secreta-de-pruebas-de-al-menos-32-bytes-de-largo";

    private FiltroAutenticacionJwt filtro;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filtro = new FiltroAutenticacionJwt(SECRETO, List.of("/api/auth", "/ws-juego"));
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    private String tokenValido() {
        SecretKey clave = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("id-de-usuario-de-prueba")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(clave)
                .compact();
    }

    @Test
    void dejaPasarRutasPublicasSinToken() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login"));

        filtro.filter(exchange, chain).block();

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void dejaPasarPreflightCorsSinImportarLaRuta() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/bancos/mios"));

        filtro.filter(exchange, chain).block();

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void rechazaRutaProtegidaSinCabeceraDeAutorizacion() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/bancos/mios"));

        filtro.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, times(0)).filter(any());
    }

    @Test
    void rechazaRutaProtegidaConTokenInvalido() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/bancos/mios")
                        .header("Authorization", "Bearer token-invalido"));

        filtro.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, times(0)).filter(any());
    }

    @Test
    void dejaPasarRutaProtegidaConTokenValido() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/bancos/mios")
                        .header("Authorization", "Bearer " + tokenValido()));

        filtro.filter(exchange, chain).block();

        verify(chain, times(1)).filter(exchange);
    }
}
