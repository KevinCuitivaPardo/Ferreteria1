package com.ferreteria.pedidos.domain;

import com.ferreteria.shared.NegocioException;

/** La foto que el domiciliario toma al entregar. Solo se aceptan imágenes JPG, PNG o WEBP de hasta 8 MB. */
public record FotoEntrega(byte[] contenido, String tipoContenido) {

    public static final int TAMANO_MAXIMO = 8 * 1024 * 1024;

    /**
     * Revisa el contenido real del archivo (no solo lo que dice el navegador) para no guardar
     * cualquier cosa como si fuera una imagen.
     *
     * @throws NegocioException si está vacía, pesa demasiado o no es una imagen válida
     */
    public static FotoEntrega deBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new NegocioException("La foto está vacía.");
        }
        if (bytes.length > TAMANO_MAXIMO) {
            throw new NegocioException("La foto pesa demasiado (máximo 8 MB).");
        }
        return new FotoEntrega(bytes, detectarTipo(bytes));
    }

    private static String detectarTipo(byte[] b) {
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        throw new NegocioException("La foto debe ser una imagen JPG, PNG o WEBP.");
    }
}
