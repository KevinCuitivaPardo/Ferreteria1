// Avisos en pantalla cuando el servidor los empuja: banner, sonido, vibración y notificación del navegador.
(function () {
    'use strict';

    const contenedor = document.getElementById('avisos');
    if (!contenedor || !window.FerreteriaTiempoReal) return;

    // ---------- Banner ----------

    function mostrar(aviso) {
        const banner = document.createElement('div');
        banner.className = 'aviso-flotante';
        banner.setAttribute('role', 'alert');

        const icono = document.createElement('span');
        icono.className = 'aviso-flotante-icono';
        icono.textContent = aviso.tipo === 'asignacion' ? '🛵' : (aviso.tipo === 'admin' ? '🔔' : 'ℹ️');

        const texto = document.createElement('div');
        texto.className = 'min-w-0 flex-1';
        const titulo = document.createElement('strong');
        titulo.textContent = aviso.titulo;
        const detalle = document.createElement('p');
        detalle.textContent = aviso.mensaje;
        texto.append(titulo, detalle);

        banner.append(icono, texto);

        if (aviso.enlace) {
            const ver = document.createElement('a');
            ver.href = aviso.enlace;
            ver.className = 'btn btn-primary btn-sm shrink-0';
            ver.textContent = 'Ver';
            banner.appendChild(ver);
        }

        const cerrar = document.createElement('button');
        cerrar.type = 'button';
        cerrar.className = 'btn btn-ghost btn-sm shrink-0';
        cerrar.setAttribute('aria-label', 'Cerrar aviso');
        cerrar.textContent = '✕';
        cerrar.addEventListener('click', () => banner.remove());
        banner.appendChild(cerrar);

        contenedor.prepend(banner);
        setTimeout(() => banner.remove(), 12000);
    }

    // ---------- Sonido y vibración ----------

    let audio = null;
    function sonar() {
        try {
            audio = audio || new (window.AudioContext || window.webkitAudioContext)();
            const oscilador = audio.createOscillator();
            const volumen = audio.createGain();
            oscilador.type = 'sine';
            oscilador.frequency.setValueAtTime(880, audio.currentTime);
            oscilador.frequency.setValueAtTime(1175, audio.currentTime + 0.14);
            volumen.gain.setValueAtTime(0.0001, audio.currentTime);
            volumen.gain.exponentialRampToValueAtTime(0.25, audio.currentTime + 0.02);
            volumen.gain.exponentialRampToValueAtTime(0.0001, audio.currentTime + 0.4);
            oscilador.connect(volumen).connect(audio.destination);
            oscilador.start();
            oscilador.stop(audio.currentTime + 0.42);
        } catch (error) {
            // el navegador bloqueó el audio hasta que la persona toque la pantalla: no pasa nada
        }
        if (navigator.vibrate) navigator.vibrate([200, 100, 200]);
    }

    // ---------- Notificación del navegador (cuando la pestaña no se está viendo) ----------

    function notificar(aviso) {
        if (!('Notification' in window) || Notification.permission !== 'granted' || !document.hidden) return;
        const n = new Notification(aviso.titulo, { body: aviso.mensaje, tag: aviso.tipo });
        n.onclick = () => { window.focus(); if (aviso.enlace) location.href = aviso.enlace; n.close(); };
    }

    const boton = document.getElementById('activar-avisos');
    if (boton && 'Notification' in window && Notification.permission === 'default') {
        boton.hidden = false;
        boton.addEventListener('click', async () => {
            await Notification.requestPermission();
            boton.hidden = true;
        });
    }

    // ---------- Qué hacer con cada aviso ----------

    FerreteriaTiempoReal.escuchar((mensaje) => {
        if (mensaje.tipo !== 'aviso') return;
        const aviso = mensaje.datos;

        mostrar(aviso);
        sonar();
        notificar(aviso);

        // La lista de entregas se actualiza sola cuando cambia la cola
        if ((aviso.tipo === 'asignacion' || aviso.tipo === 'cola') && location.pathname === '/Entregas') {
            setTimeout(() => location.reload(), 1500);
        }
    });
})();
