// El teléfono del domiciliario comparte su ubicación (GPS) con el servidor mientras la opción está activa.
// Avisa a los demás scripts con el evento "posicion-propia" para que dibujen "tú estás aquí" y recalculen la ruta.
(function () {
    'use strict';

    const boton = document.getElementById('compartir-ubicacion');
    if (!boton || !window.FerreteriaTiempoReal || !navigator.geolocation) {
        if (boton) boton.hidden = true;
        return;
    }

    const CLAVE = 'ferreteria.compartirUbicacion';
    const etiqueta = boton.querySelector('[data-estado]');
    const INTERVALO_MINIMO_MS = 4000;

    let observador = null;
    let ultimoEnvio = 0;
    let pantallaActiva = null;

    function guardado() {
        try { return localStorage.getItem(CLAVE) !== 'no'; } catch (error) { return true; }
    }

    function recordar(valor) {
        try { localStorage.setItem(CLAVE, valor ? 'si' : 'no'); } catch (error) { /* sin almacenamiento */ }
    }

    function mostrar(activo, texto) {
        boton.setAttribute('aria-pressed', String(activo));
        boton.classList.toggle('compartiendo', activo);
        if (etiqueta) etiqueta.textContent = texto;
    }

    async function mantenerPantallaEncendida() {
        try {
            if ('wakeLock' in navigator) pantallaActiva = await navigator.wakeLock.request('screen');
        } catch (error) { /* no es esencial */ }
    }

    function iniciar() {
        if (observador !== null) return;
        mostrar(true, 'Buscando GPS…');

        observador = navigator.geolocation.watchPosition((pos) => {
            const punto = { latitud: pos.coords.latitude, longitud: pos.coords.longitude };
            window.dispatchEvent(new CustomEvent('posicion-propia', { detail: punto }));

            const ahora = Date.now();
            if (ahora - ultimoEnvio < INTERVALO_MINIMO_MS) return;
            ultimoEnvio = ahora;

            const enviado = FerreteriaTiempoReal.enviar({
                tipo: 'posicion', latitud: punto.latitud, longitud: punto.longitud, precision: pos.coords.accuracy,
            });
            mostrar(true, enviado ? 'Compartiendo ubicación' : 'Reconectando…');
        }, (error) => {
            mostrar(true, error.code === 1 ? 'Permiso de ubicación denegado' : 'Sin señal de GPS');
        }, { enableHighAccuracy: true, maximumAge: 5000, timeout: 20000 });

        mantenerPantallaEncendida();
    }

    function detener() {
        if (observador !== null) navigator.geolocation.clearWatch(observador);
        observador = null;
        if (pantallaActiva) { pantallaActiva.release().catch(() => {}); pantallaActiva = null; }
        mostrar(false, 'Ubicación apagada');
    }

    boton.addEventListener('click', () => {
        const activar = observador === null;
        recordar(activar);
        if (activar) iniciar(); else detener();
    });

    // Al volver a la pestaña se recupera la pantalla encendida (el navegador la suelta al ocultarla)
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden && observador !== null) mantenerPantallaEncendida();
    });

    FerreteriaTiempoReal.escuchar((m) => {
        if (m.tipo === 'conexion' && observador !== null) {
            mostrar(true, m.datos.conectado ? 'Compartiendo ubicación' : 'Reconectando…');
        }
    });

    if (guardado()) iniciar(); else mostrar(false, 'Ubicación apagada');
})();
