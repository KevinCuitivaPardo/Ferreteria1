// Ayudas para los mapas (Leaflet + OpenStreetMap). Lo usan todas las pantallas con mapa.
window.FerreteriaMapa = (function () {
    'use strict';

    // Bogotá, por si todavía no hay ningún punto que mostrar
    const CENTRO_POR_DEFECTO = [4.711, -74.0721];

    // Mismos colores que las píldoras de estado de los pedidos
    const COLORES_ESTADO = {
        pendiente: '#8e8e93',
        alistado: '#007aff',
        en_ruta: '#ff9500',
        entregado: '#34c759',
        no_entregado: '#ff3b30',
        cancelado: '#af52de',
    };

    function crear(elemento, opciones) {
        const centro = (opciones && opciones.centro) || CENTRO_POR_DEFECTO;
        const zoom = (opciones && opciones.zoom) || 12;

        const mapa = L.map(elemento, { scrollWheelZoom: false }).setView(centro, zoom);
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        }).addTo(mapa);

        // La rueda del ratón solo acerca el mapa si se hizo clic en él (para no atrapar el scroll de la página)
        mapa.on('click', () => mapa.scrollWheelZoom.enable());
        mapa.on('mouseout', () => mapa.scrollWheelZoom.disable());
        return mapa;
    }

    function pin(color, esSucursal) {
        return L.divIcon({
            className: 'pin-contenedor',
            html: '<span class="pin' + (esSucursal ? ' pin-sucursal' : '') + '" style="--pin:' + color + '"></span>',
            iconSize: [26, 26],
            iconAnchor: esSucursal ? [13, 13] : [5, 30],
            popupAnchor: esSucursal ? [0, -12] : [8, -30],
        });
    }

    function colorDeEstado(estado) {
        return COLORES_ESTADO[estado] || COLORES_ESTADO.pendiente;
    }

    function escapar(texto) {
        const div = document.createElement('div');
        div.textContent = texto == null ? '' : String(texto);
        return div.innerHTML;
    }

    /**
     * Dibuja la ruta en carro entre dos puntos con OSRM (servicio gratuito basado en OpenStreetMap).
     * Si el servicio no responde, deja una línea recta punteada. Devuelve { distanciaKm, minutos, aproximada }.
     */
    async function ruta(mapa, desde, hasta) {
        const estilo = { color: '#007aff', weight: 5, opacity: 0.85 };
        try {
            const url = 'https://router.project-osrm.org/route/v1/driving/'
                + desde[1] + ',' + desde[0] + ';' + hasta[1] + ',' + hasta[0]
                + '?overview=full&geometries=geojson';
            const respuesta = await fetch(url);
            const datos = await respuesta.json();
            if (datos.code !== 'Ok' || !datos.routes.length) throw new Error('sin ruta');

            const camino = datos.routes[0];
            const linea = L.geoJSON(camino.geometry, { style: estilo }).addTo(mapa);
            return { linea, distanciaKm: camino.distance / 1000, minutos: camino.duration / 60, aproximada: false };
        } catch (error) {
            const linea = L.polyline([desde, hasta], { ...estilo, dashArray: '8 10' }).addTo(mapa);
            const metros = mapa.distance(desde, hasta);
            return { linea, distanciaKm: metros / 1000, minutos: null, aproximada: true };
        }
    }

    function describirRuta(info) {
        const km = info.distanciaKm.toLocaleString('es-CO', { maximumFractionDigits: 1 });
        if (info.aproximada) return 'Distancia en línea recta: ' + km + ' km (no se pudo calcular la ruta por calles).';
        return 'Ruta: ' + km + ' km · aprox. ' + Math.max(1, Math.round(info.minutos)) + ' min en carro.';
    }

    return { crear, pin, colorDeEstado, escapar, ruta, describirRuta, CENTRO_POR_DEFECTO };
})();
