// Formulario de sucursal: se marca el punto en el mapa (tocándolo o buscando la dirección).
(function () {
    'use strict';

    const elemento = document.getElementById('mapa-sucursal');
    if (!elemento || !window.L) return;

    const latitud = document.getElementById('latitud');
    const longitud = document.getElementById('longitud');
    const direccion = document.getElementById('direccion');
    const estado = document.getElementById('estado-mapa');
    const boton = document.getElementById('buscar-direccion');

    const inicial = latitud.value && longitud.value ? [Number(latitud.value), Number(longitud.value)] : null;
    const mapa = FerreteriaMapa.crear(elemento, { centro: inicial || undefined, zoom: inicial ? 16 : 12 });
    let marcador = null;

    function marcar(punto, centrar) {
        latitud.value = punto[0].toFixed(7);
        longitud.value = punto[1].toFixed(7);
        if (!marcador) {
            marcador = L.marker(punto, { draggable: true, icon: FerreteriaMapa.pin('#007aff', true) }).addTo(mapa);
            marcador.on('dragend', () => {
                const p = marcador.getLatLng();
                marcar([p.lat, p.lng], false);
            });
        } else {
            marcador.setLatLng(punto);
        }
        if (centrar) mapa.setView(punto, Math.max(mapa.getZoom(), 16));
        estado.textContent = 'Ubicación marcada. Puedes arrastrar el punto para afinarla.';
    }

    if (inicial) marcar(inicial, false);

    mapa.on('click', (e) => marcar([e.latlng.lat, e.latlng.lng], false));

    boton.addEventListener('click', async () => {
        const texto = direccion.value.trim();
        if (!texto) {
            estado.textContent = 'Escribe primero la dirección.';
            direccion.focus();
            return;
        }

        boton.disabled = true;
        estado.textContent = 'Buscando en OpenStreetMap...';
        try {
            const respuesta = await fetch('/api/geocodificar?q=' + encodeURIComponent(texto));
            const datos = await respuesta.json();
            if (datos.encontrado) {
                marcar([datos.latitud, datos.longitud], true);
                estado.textContent = 'Encontrada: ' + datos.descripcion + '. Revisa que el punto sea el correcto.';
            } else {
                estado.textContent = 'No se encontró esa dirección. Toca el mapa para marcar el punto a mano.';
            }
        } catch (error) {
            estado.textContent = 'No se pudo buscar la dirección. Toca el mapa para marcar el punto a mano.';
        } finally {
            boton.disabled = false;
        }
    });
})();
