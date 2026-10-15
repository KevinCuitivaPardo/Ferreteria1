package com.ferreteria.tiemporeal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ferreteria.SimuladorDeAdaptadores;
import com.ferreteria.tiemporeal.application.port.in.ConsultarPosiciones;
import com.ferreteria.tiemporeal.application.port.in.PublicarAvisos;
import com.ferreteria.tiemporeal.domain.Aviso;
import com.ferreteria.usuarios.application.port.in.ConsultarUsuarios;
import com.ferreteria.usuarios.domain.Rol;
import com.ferreteria.usuarios.domain.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Prueba de punta a punta del tiempo real con un servidor de verdad: se inicia sesión, se abre el WebSocket
 * con la cookie de esa sesión y se comprueba quién recibe qué.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TiempoRealWebSocketTest extends SimuladorDeAdaptadores {

    @LocalServerPort int puerto;
    @Autowired ConsultarPosiciones posiciones;
    @Autowired PublicarAvisos avisos;
    @Autowired ObjectMapper json;
    @Autowired ConsultarUsuarios usuarios; // el simulacro de usuarios

    private final List<WebSocket> abiertos = new ArrayList<>();

    @BeforeEach
    void usuariosDePrueba() {
        var clave = new BCryptPasswordEncoder().encode("clave123");
        when(usuarios.porNombre("pedro")).thenReturn(Optional.of(
                new Usuario(9, "pedro", clave, Rol.DOMICILIARIO, true, BigDecimal.ZERO, 1)));
        when(usuarios.porNombre("admin")).thenReturn(Optional.of(
                new Usuario(1, "admin", clave, Rol.ADMIN, true, BigDecimal.ZERO, null)));
    }

    @AfterEach
    void cerrar() {
        abiertos.forEach(ws -> ws.abort());
    }

    // ---------- Ayudas ----------

    /** Inicia sesión como lo hace el navegador y devuelve el valor de la cookie de sesión. */
    private String iniciarSesion(String usuario) throws Exception {
        CookieManager galletas = new CookieManager();
        HttpClient cliente = HttpClient.newBuilder().cookieHandler(galletas)
                .followRedirects(HttpClient.Redirect.NEVER).build();
        URI login = URI.create("http://localhost:" + puerto + "/Login");

        String pagina = cliente.send(HttpRequest.newBuilder(login).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
        Matcher token = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(pagina);
        assertTrue(token.find(), "la página de login debe traer el token CSRF");

        var respuesta = cliente.send(HttpRequest.newBuilder(login)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("nombreUsuario=" + usuario + "&clave=clave123&_csrf="
                        + token.group(1))).build(), HttpResponse.BodyHandlers.discarding());
        assertEquals(302, respuesta.statusCode());
        assertTrue(respuesta.headers().firstValue("Location").orElse("").endsWith("/"), "el login debe llevar al inicio");

        return galletas.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
    }

    private static class Buzon implements WebSocket.Listener {
        final BlockingQueue<JsonNode> recibidos = new LinkedBlockingQueue<>();
        private final ObjectMapper json;
        private final StringBuilder parcial = new StringBuilder();

        Buzon(ObjectMapper json) {
            this.json = json;
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence datos, boolean ultimo) {
            parcial.append(datos);
            if (ultimo) {
                try {
                    recibidos.add(json.readTree(parcial.toString()));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                parcial.setLength(0);
            }
            ws.request(1);
            return null;
        }

        /** El próximo mensaje de ese tipo (se saltan los demás), o null si no llega en 4 segundos. */
        JsonNode siguiente(String tipo) throws InterruptedException {
            long limite = System.currentTimeMillis() + 4000;
            while (System.currentTimeMillis() < limite) {
                JsonNode m = recibidos.poll(200, TimeUnit.MILLISECONDS);
                if (m != null && tipo.equals(m.path("tipo").asText())) {
                    return m;
                }
            }
            return null;
        }
    }

    private WebSocket conectar(String sesion, Buzon buzon) {
        WebSocket ws = HttpClient.newHttpClient().newWebSocketBuilder()
                .header("Cookie", "JSESSIONID=" + sesion)
                .buildAsync(URI.create("ws://localhost:" + puerto + "/ws"), buzon).join();
        abiertos.add(ws);
        ws.request(1);
        return ws;
    }

    // ---------- Pruebas ----------

    @Test
    void sinSesionNoSePuedeAbrirElCanal() {
        var intento = HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:" + puerto + "/ws"), new Buzon(json));

        assertThrows(CompletionException.class, intento::join);
    }

    @Test
    void laPosicionDelDomiciliarioLlegaEnVivoAlAdmin() throws Exception {
        var buzonAdmin = new Buzon(json);
        var buzonDomiciliario = new Buzon(json);
        conectar(iniciarSesion("admin"), buzonAdmin);
        WebSocket domiciliario = conectar(iniciarSesion("pedro"), buzonDomiciliario);

        domiciliario.sendText("{\"tipo\":\"posicion\",\"latitud\":4.65,\"longitud\":-74.06,\"precision\":8.5}", true).join();

        JsonNode mensaje = buzonAdmin.siguiente("posicion");
        assertTrue(mensaje != null, "el admin debe recibir la posición");
        assertEquals("pedro", mensaje.path("datos").path("nombre").asText());
        assertEquals(9, mensaje.path("datos").path("usuarioId").asInt());
        assertEquals(4.65, mensaje.path("datos").path("latitud").asDouble());
        assertEquals(-74.06, mensaje.path("datos").path("longitud").asDouble());

        // Queda guardada para quien abra el mapa después
        assertEquals(1, posiciones.actuales().size());
        assertEquals("pedro", posiciones.actuales().get(0).nombre());

        // El domiciliario no recibe las posiciones de nadie
        assertNull(buzonDomiciliario.siguiente("posicion"));
    }

    @Test
    void laPosicionDeAlguienQueNoEsDomiciliarioSeIgnora() throws Exception {
        var buzonAdmin = new Buzon(json);
        WebSocket admin = conectar(iniciarSesion("admin"), buzonAdmin);

        admin.sendText("{\"tipo\":\"posicion\",\"latitud\":4.65,\"longitud\":-74.06}", true).join();

        assertNull(buzonAdmin.siguiente("posicion"));
        assertTrue(posiciones.actuales().stream().noneMatch(p -> p.usuarioId() == 1), "el admin no es un conductor");
    }

    @Test
    void losMensajesBasuraNoRompenLaConexion() throws Exception {
        var buzonAdmin = new Buzon(json);
        var buzonDomiciliario = new Buzon(json);
        conectar(iniciarSesion("admin"), buzonAdmin);
        WebSocket domiciliario = conectar(iniciarSesion("pedro"), buzonDomiciliario);

        domiciliario.sendText("esto no es json", true).join();
        domiciliario.sendText("{\"tipo\":\"posicion\",\"latitud\":\"abc\",\"longitud\":1}", true).join();
        domiciliario.sendText("{\"tipo\":\"ping\"}", true).join();
        domiciliario.sendText("{\"tipo\":\"posicion\",\"latitud\":95,\"longitud\":1}", true).join();
        domiciliario.sendText("{\"tipo\":\"posicion\",\"latitud\":4.7,\"longitud\":-74.1}", true).join();

        JsonNode mensaje = buzonAdmin.siguiente("posicion");
        assertTrue(mensaje != null, "la posición válida debe llegar después de la basura");
        assertEquals(4.7, mensaje.path("datos").path("latitud").asDouble());
    }

    @Test
    void laAlertaDeAsignacionLlegaSoloAlDomiciliarioIndicado() throws Exception {
        var buzonAdmin = new Buzon(json);
        var buzonDomiciliario = new Buzon(json);
        conectar(iniciarSesion("admin"), buzonAdmin);
        conectar(iniciarSesion("pedro"), buzonDomiciliario);

        avisos.avisarAUsuario(9, new Aviso("asignacion", "Nuevo pedido asignado", "PED-1 · Luis", "/Entregas"));

        JsonNode aviso = buzonDomiciliario.siguiente("aviso");
        assertTrue(aviso != null, "el domiciliario debe recibir la alerta");
        assertEquals("asignacion", aviso.path("datos").path("tipo").asText());
        assertEquals("Nuevo pedido asignado", aviso.path("datos").path("titulo").asText());
        assertEquals("/Entregas", aviso.path("datos").path("enlace").asText());
        assertNull(buzonAdmin.siguiente("aviso"));
    }

    @Test
    void losAvisosParaAdminsLlegansoloAlosAdmins() throws Exception {
        var buzonAdmin = new Buzon(json);
        var buzonDomiciliario = new Buzon(json);
        conectar(iniciarSesion("admin"), buzonAdmin);
        conectar(iniciarSesion("pedro"), buzonDomiciliario);

        avisos.avisarAAdmins(new Aviso("admin", "Nuevo pedido por asignar", "PED-2", "/Pedidos/Detalle/2"));

        assertTrue(buzonAdmin.siguiente("aviso") != null);
        assertNull(buzonDomiciliario.siguiente("aviso"));
    }

    @Test
    void siElDomiciliarioTieneVariasPantallasAbiertasLasRecibenTodas() throws Exception {
        var primera = new Buzon(json);
        var segunda = new Buzon(json);
        conectar(iniciarSesion("pedro"), primera);
        conectar(iniciarSesion("pedro"), segunda);

        avisos.avisarAUsuario(9, new Aviso("cola", "Cambió tu lista de pedidos", "Te quitaron el pedido PED-3", "/Entregas"));

        assertTrue(primera.siguiente("aviso") != null);
        assertTrue(segunda.siguiente("aviso") != null);
    }
}
