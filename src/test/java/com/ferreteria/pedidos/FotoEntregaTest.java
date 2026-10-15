package com.ferreteria.pedidos;

import com.ferreteria.pedidos.domain.FotoEntrega;
import com.ferreteria.shared.NegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Solo se aceptan imágenes de verdad: se revisa el contenido del archivo, no lo que diga el navegador. */
class FotoEntregaTest {

    @Test
    void reconoceJpegPngYWebp() {
        assertEquals("image/jpeg", FotoEntrega.deBytes(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0}).tipoContenido());
        assertEquals("image/png", FotoEntrega.deBytes(
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0}).tipoContenido());
        assertEquals("image/webp", FotoEntrega.deBytes(
                new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V'}).tipoContenido());
    }

    @Test
    void rechazaLoQueNoEsUnaImagen() {
        assertEquals("La foto debe ser una imagen JPG, PNG o WEBP.",
                assertThrows(NegocioException.class,
                        () -> FotoEntrega.deBytes("<script>alert(1)</script>".getBytes())).getMessage());
        assertThrows(NegocioException.class, () -> FotoEntrega.deBytes(new byte[]{'M', 'Z', 0, 0, 0, 0}));
    }

    @Test
    void rechazaVaciasYDemasiadoPesadas() {
        assertEquals("La foto está vacía.", assertThrows(NegocioException.class, () -> FotoEntrega.deBytes(new byte[0])).getMessage());
        assertEquals("La foto está vacía.", assertThrows(NegocioException.class, () -> FotoEntrega.deBytes(null)).getMessage());

        byte[] enorme = new byte[FotoEntrega.TAMANO_MAXIMO + 1];
        enorme[0] = (byte) 0xFF;
        enorme[1] = (byte) 0xD8;
        enorme[2] = (byte) 0xFF;
        assertEquals("La foto pesa demasiado (máximo 8 MB).",
                assertThrows(NegocioException.class, () -> FotoEntrega.deBytes(enorme)).getMessage());
    }
}
