// Conexión en tiempo real (WebSocket) con el servidor. La abre toda pantalla con sesión iniciada y la
// comparten los demás scripts: avisos, GPS del domiciliario y mapa en vivo del administrador.
window.FerreteriaTiempoReal = (function () {
    'use strict';

    const escuchas = new Set();
    let socket = null;
    let intentos = 0;
    let conectado = false;

    if (!document.body || !document.body.dataset.rol) {
        // Sin sesión (login): no hay nada que escuchar
        return { escuchar: () => () => {}, enviar: () => false, estaConectado: () => false };
    }

    function direccion() {
        return (location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws';
    }

    function conectar() {
        socket = new WebSocket(direccion());

        socket.onopen = () => {
            conectado = true;
            intentos = 0;
            avisar({ tipo: 'conexion', datos: { conectado: true } });
        };

        socket.onmessage = (evento) => {
            try {
                avisar(JSON.parse(evento.data));
            } catch (error) {
                // mensaje que no se entiende: se ignora
            }
        };

        socket.onclose = () => {
            const estabaConectado = conectado;
            conectado = false;
            if (estabaConectado) avisar({ tipo: 'conexion', datos: { conectado: false } });
            // Reintenta con espera creciente (1 s, 2 s, 4 s... hasta 30 s)
            const espera = Math.min(30000, 1000 * Math.pow(2, intentos++));
            setTimeout(conectar, espera);
        };

        socket.onerror = () => socket.close();
    }

    function avisar(mensaje) {
        escuchas.forEach((funcion) => {
            try { funcion(mensaje); } catch (error) { console.error(error); }
        });
    }

    /** Recibe cada mensaje del servidor: {tipo, datos}. Devuelve una función para dejar de escuchar. */
    function escuchar(funcion) {
        escuchas.add(funcion);
        return () => escuchas.delete(funcion);
    }

    function enviar(objeto) {
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify(objeto));
            return true;
        }
        return false;
    }

    // Mantiene viva la conexión cuando no hay otro tráfico
    setInterval(() => enviar({ tipo: 'ping' }), 25000);

    conectar();
    return { escuchar, enviar, estaConectado: () => conectado };
})();
