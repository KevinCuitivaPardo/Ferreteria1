package com.ferreteria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Quién puede entrar a cada ruta. Los usuarios los aporta el módulo de usuarios (DetallesUsuarioService).
 *
 * <ul>
 *   <li><b>admin</b>: todo, en todas las sucursales.</li>
 *   <li><b>vendedor</b>: inventario, ventas y pedidos de su sucursal.</li>
 *   <li><b>domiciliario</b>: solo las entregas que le asignan.</li>
 * </ul>
 */
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/Login", "/css/**", "/js/**", "/vendor/**", "/img/**", "/favicon.ico",
                                "/error").permitAll()
                        .requestMatchers("/Nuevo", "/Editar/**", "/Ajustar/**", "/Usuarios/**", "/Descuentos/**",
                                "/Sucursales/**", "/Eventos", "/Pedidos/Mapa/**", "/Productos/*/eliminar",
                                "/Pedidos/Detalle/*/estado", "/Pedidos/Detalle/*/domiciliario",
                                "/Pedidos/Detalle/*/ubicacion").hasRole("admin")
                        .requestMatchers("/Entregas/**").hasRole("domiciliario")
                        .requestMatchers("/Seguimiento/**").hasRole("admin")
                        .requestMatchers("/Productos/**", "/Movimientos/**", "/Ventas/**", "/Pedidos/**",
                                "/api/geocodificar").hasAnyRole("admin", "vendedor")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/Login")
                        .loginProcessingUrl("/Login")
                        .usernameParameter("nombreUsuario")
                        .passwordParameter("clave")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/Login?error"))
                // Cerrar sesión solo se hace con POST (con token): un enlace o una imagen ajena no puede
                // sacar al usuario. Un GET a /Salir muestra la pantalla de confirmación.
                .logout(logout -> logout
                        .logoutUrl("/Salir")
                        .logoutSuccessUrl("/Login"))
                .exceptionHandling(ex -> ex.accessDeniedPage("/acceso-denegado"));

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // Lee los hashes BCrypt ya guardados en la tabla usuarios.
        return new BCryptPasswordEncoder();
    }
}
