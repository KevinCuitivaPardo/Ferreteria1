// Mapa de un domicilio: sucursal de salida, dirección de entrega y ruta.
// El elemento con id "mapa-pedido" trae los datos en atributos data-*. Según la pantalla además:
//  - Administrador / vendedor (pedido en ruta): se ve el conductor moviéndose en vivo (data-conductor-id).
//  - Domiciliario (data-modo="conductor"): se dibuja "tú estás aquí" y, en ruta, la ruta sale desde su posición.
//  - Administrador: formulario "form-ubicacion" para fijar a mano el punto de entrega.
(function () {
    'use strict';

    const elemento = document.getElementById('mapa-pedido');
    if (!elemento || !window.L) return;

    const numero = (valor) => (valor === undefined || valor === '' ? null : Number(valor));
    const lat = numero(elemento.dataset.lat);
    const lng = numero(elemento.dataset.lng);
    const origenLat = numero(elemento.dataset.origenLat);
    const origenLng = numero(elemento.dataset.origenLng);
    const destino = lat !== null && lng !== null ? [lat, lng] : null;
    const origen = origenLat !== null && origenLng !== null ? [origenLat, origenLng] : null;
    const modoConductor = elemento.dataset.modo === 'conductor';
    const enRuta = elemento.dataset.estado === 'en_ruta';

    const mapa = FerreteriaMapa.crear(elemento, { centro: destino || origen || undefined, zoom: 14 });
    const resumen = document.getElementById('resumen-ruta');
    const puntos = [];
    let marcadorDestino = null;
    let lineaRuta = null;

    // ---------- Puntos fijos ----------

    function ponerDestino(punto) {
        if (marcadorDestino) {
            marcadorDestino.setLatLng(punto);
            return;
        }
        marcadorDestino = L.marker(punto, { icon: FerreteriaMapa.pin(FerreteriaMapa.colorDeEstado(elemento.dataset.estado)) })
            .addTo(mapa)
            .bindPopup('<strong>' + FerreteriaMapa.escapar(elemento.dataset.cliente) + '</strong><br>'
                + FerreteriaMapa.escapar(elemento.dataset.direccion));
    }

    if (origen) {
        L.marker(origen, { icon: FerreteriaMapa.pin('#007aff', true) }).addTo(mapa)
            .bindPopup('<strong>' + FerreteriaMapa.escapar(elemento.dataset.sucursal) + '</strong><br>Sucursal de salida');
        puntos.push(origen);
    }
    if (destino) {
        ponerDestino(destino);
        puntos.push(destino);
    }
    if (puntos.length > 1) mapa.fitBounds(L.latLngBounds(puntos), { padding: [40, 40] });

    // ---------- Ruta ----------

    async function trazarRuta(desde, hasta, ajustarVista) {
        const info = await FerreteriaMapa.ruta(mapa, desde, hasta);
        if (lineaRuta) mapa.removeLayer(lineaRuta);
        lineaRuta = info.linea;
        if (resumen) resumen.textContent = FerreteriaMapa.describirRuta(info);
        if (ajustarVista && info.linea && info.linea.getBounds) mapa.fitBounds(info.linea.getBounds(), { padding: [40, 40] });
    }

    if (origen && destino && !(modoConductor && enRuta)) {
        trazarRuta(origen, destino, true);
    } else if (resumen && !(modoConductor && enRuta)) {
        resumen.textContent = !destino
            ? 'No se pudo ubicar esta dirección en el mapa.'
            : 'La sucursal de salida todavía no tiene ubicación en el mapa.';
    }

    // ---------- Domiciliario: "tú estás aquí" y ruta desde su posición ----------

    if (modoConductor) {
        let yo = null;
        let origenRuta = null;
        let ultimoCalculo = 0;

        const kilometros = (a, b) => mapa.distance(a, b) / 1000;

        window.addEventListener('posicion-propia', (e) => {
            const aqui = [e.detail.latitud, e.detail.longitud];

            if (!yo) {
                yo = L.marker(aqui, {
                    icon: L.divIcon({ className: 'pin-contenedor', html: '<span class="yo-aqui"></span>', iconSize: [16, 16] }),
                    zIndexOffset: 1000,
                }).addTo(mapa).bindPopup('Tú estás aquí');
            } else {
                yo.setLatLng(aqui);
            }

            if (enRuta && destino) {
                const ahora = Date.now();
                const primera = origenRuta === null;
                const seMovio = !primera && kilometros(origenRuta, aqui) > 0.2 && ahora - ultimoCalculo > 45000;
                if (primera || seMovio) {
                    origenRuta = aqui;
                    ultimoCalculo = ahora;
                    trazarRuta(aqui, destino, primera);
                }
            }
        });

        if (enRuta && resumen) resumen.textContent = 'Buscando tu ubicación para trazar la ruta…';
    }

    // ---------- Administrador / vendedor: conductor en vivo ----------

    const conductorId = numero(elemento.dataset.conductorId);
    if (conductorId !== null && window.FerreteriaTiempoReal) {
        let marcador = null;
        const nombre = elemento.dataset.conductor || 'Conductor';

        function moverConductor(p) {
            const punto = [p.latitud, p.longitud];
            if (!marcador) {
                marcador = L.marker(punto, {
                    icon: L.divIcon({
                        className: 'pin-contenedor',
                        html: '<span class="conductor">' + FerreteriaMapa.escapar(nombre) + '</span>',
                        iconSize: [0, 0],
                    }),
                    zIndexOffset: 900,
                }).addTo(mapa);
            } else {
                marcador.setLatLng(punto);
            }
        }

        fetch('/Seguimiento/posiciones').then((r) => (r.ok ? r.json() : [])).then((lista) => {
            const mia = lista.find((p) => p.usuarioId === conductorId);
            if (mia) moverConductor(mia);
        }).catch(() => {});

        FerreteriaTiempoReal.escuchar((m) => {
            if (m.tipo === 'posicion' && m.datos.usuarioId === conductorId) moverConductor(m.datos);
        });
    }

    // ---------- Administrador: ajuste manual del punto de entrega ----------

    const formulario = document.getElementById('form-ubicacion');
    if (!formulario) return;

    const botonAjustar = document.getElementById('ajustar-ubicacion');
    const botonGuardar = document.getElementById('guardar-ubicacion');
    const ayuda = document.getElementById('ayuda-ubicacion');
    const campoLat = document.getElementById('nueva-latitud');
    const campoLng = document.getElementById('nueva-longitud');
    let ajustando = false;

    function fijar(punto) {
        campoLat.value = punto[0].toFixed(7);
        campoLng.value = punto[1].toFixed(7);
        ponerDestino(punto);
        if (marcadorDestino.dragging) marcadorDestino.dragging.enable();
        botonGuardar.disabled = false;
        ayuda.textContent = 'Punto marcado. Puedes arrastrarlo para afinarlo; luego guarda la ubicación.';
    }

    botonAjustar.addEventListener('click', () => {
        ajustando = true;
        botonAjustar.hidden = true;
        botonGuardar.hidden = false;
        botonGuardar.disabled = true;
        ayuda.textContent = 'Toca el mapa en el lugar exacto de la entrega.';
        if (marcadorDestino) {
            marcadorDestino.dragging.enable();
            marcadorDestino.on('dragend', () => {
                const p = marcadorDestino.getLatLng();
                fijar([p.lat, p.lng]);
            });
        }
    });

    mapa.on('click', (e) => {
        if (ajustando) fijar([e.latlng.lat, e.latlng.lng]);
    });
})();
