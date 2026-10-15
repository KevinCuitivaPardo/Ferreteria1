// Terminar una entrega: la foto es obligatoria si se entregó, el motivo si no se pudo, y la foto se
// reduce antes de subirla (las fotos del celular pesan varios MB y el domiciliario suele tener datos móviles).
(function () {
    'use strict';

    const formulario = document.getElementById('form-terminar');
    if (!formulario) return;

    const radios = formulario.querySelectorAll('input[name="resultado"]');
    const foto = document.getElementById('foto');
    const vista = document.getElementById('vista-foto');
    const marcaFoto = document.getElementById('foto-obligatoria');
    const marcaNota = document.getElementById('nota-obligatoria');
    const nota = document.getElementById('nota');
    const error = document.getElementById('error-terminar');
    const boton = document.getElementById('boton-terminar');

    const LADO_MAXIMO = 1600;
    const UMBRAL_BYTES = 1.2 * 1024 * 1024;
    let yaReducida = false;

    const resultado = () => formulario.querySelector('input[name="resultado"]:checked').value;

    function mostrarError(texto) {
        error.textContent = texto;
        error.hidden = !texto;
    }

    function ajustarObligatorios() {
        const entregado = resultado() === 'entregado';
        marcaFoto.hidden = !entregado;
        marcaNota.hidden = entregado;
        mostrarError('');
    }

    radios.forEach((r) => r.addEventListener('change', ajustarObligatorios));

    foto.addEventListener('change', () => {
        yaReducida = false;
        mostrarError('');
        const archivo = foto.files[0];
        if (!archivo) {
            vista.hidden = true;
            return;
        }
        vista.src = URL.createObjectURL(archivo);
        vista.hidden = false;
    });

    async function reducir(archivo) {
        let imagen;
        try {
            imagen = await createImageBitmap(archivo, { imageOrientation: 'from-image' });
        } catch (error) {
            return archivo; // formato que el navegador no sabe dibujar: se sube tal cual
        }

        const escala = Math.min(1, LADO_MAXIMO / Math.max(imagen.width, imagen.height));
        const lienzo = document.createElement('canvas');
        lienzo.width = Math.round(imagen.width * escala);
        lienzo.height = Math.round(imagen.height * escala);
        lienzo.getContext('2d').drawImage(imagen, 0, 0, lienzo.width, lienzo.height);

        const blob = await new Promise((resolver) => lienzo.toBlob(resolver, 'image/jpeg', 0.82));
        if (!blob || blob.size >= archivo.size) return archivo;
        return new File([blob], 'entrega.jpg', { type: 'image/jpeg' });
    }

    formulario.addEventListener('submit', async (e) => {
        const entregado = resultado() === 'entregado';
        const archivo = foto.files[0];

        if (entregado && !archivo) {
            e.preventDefault();
            mostrarError('Toma la foto de la entrega para poder terminarla.');
            foto.focus();
            return;
        }
        if (!entregado && !nota.value.trim()) {
            e.preventDefault();
            mostrarError('Cuenta por qué no se pudo entregar.');
            nota.focus();
            return;
        }

        if (archivo && !yaReducida && archivo.size > UMBRAL_BYTES) {
            e.preventDefault();
            boton.disabled = true;
            boton.textContent = 'Preparando la foto…';

            const reducida = await reducir(archivo);
            const contenedor = new DataTransfer();
            contenedor.items.add(reducida);
            foto.files = contenedor.files;
            yaReducida = true;

            boton.textContent = 'Subiendo…';
            formulario.submit();
            return;
        }

        boton.disabled = true;
        boton.textContent = 'Subiendo…';
    });

    ajustarObligatorios();
})();
