package com.ferreteria.pedidos.infrastructure.out.archivos;

import com.ferreteria.pedidos.application.port.out.AlmacenFotos;
import com.ferreteria.pedidos.domain.FotoEntrega;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Guarda las fotos como archivos en una carpeta (por defecto {@code datos/fotos}). El nombre lo genera el
 * sistema, nunca el usuario, y al leer se rechaza cualquier nombre que no tenga ese formato.
 */
@Component
class AlmacenFotosEnDisco implements AlmacenFotos {

    private static final Logger log = LoggerFactory.getLogger(AlmacenFotosEnDisco.class);
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[0-9a-f-]{36}\\.(jpg|png|webp)");

    private final Path carpeta;

    AlmacenFotosEnDisco(@Value("${ferreteria.fotos.directorio:datos/fotos}") String directorio) {
        this.carpeta = Path.of(directorio).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(FotoEntrega foto) {
        String nombre = UUID.randomUUID() + extensionDe(foto.tipoContenido());
        try {
            Files.createDirectories(carpeta);
            Files.write(carpeta.resolve(nombre), foto.contenido());
            return nombre;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la foto", e);
        }
    }

    @Override
    public Optional<byte[]> leer(String archivo) {
        if (!NOMBRE_VALIDO.matcher(archivo).matches()) {
            return Optional.empty();
        }
        Path ruta = carpeta.resolve(archivo).normalize();
        if (!ruta.startsWith(carpeta) || !Files.isRegularFile(ruta)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(ruta));
        } catch (IOException e) {
            log.warn("No se pudo leer la foto {}: {}", archivo, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void borrar(String archivo) {
        if (!NOMBRE_VALIDO.matcher(archivo).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(carpeta.resolve(archivo));
        } catch (IOException e) {
            log.warn("No se pudo borrar la foto {}: {}", archivo, e.getMessage());
        }
    }

    private static String extensionDe(String tipoContenido) {
        return switch (tipoContenido) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
