// Comportamientos comunes a todas las pantallas.
(function () {
    'use strict';

    // ---------- Cuadro de confirmación ----------
    // <form data-confirmar="mensaje"> pide confirmación antes de enviarse. Opcionales:
    //   data-confirmar-titulo="¿Cerrar sesión?"   data-confirmar-boton="Cerrar sesión"   data-confirmar-peligro
    const dialogo = document.getElementById('dialogo-confirmar');
    const formularioPendiente = { actual: null };

    function abrirConfirmacion(formulario) {
        const d = formulario.dataset;
        document.getElementById('confirmar-titulo').textContent = d.confirmarTitulo || '¿Estás seguro?';
        document.getElementById('confirmar-mensaje').textContent = d.confirmar;

        const aceptar = document.getElementById('confirmar-aceptar');
        aceptar.textContent = d.confirmarBoton || 'Aceptar';
        aceptar.classList.toggle('peligro', d.confirmarPeligro !== undefined);

        formularioPendiente.actual = formulario;
        dialogo.returnValue = 'cancelar';
        dialogo.showModal();
        document.getElementById('confirmar-cancelar').focus();
    }

    if (dialogo && typeof dialogo.showModal === 'function') {
        // En fase de captura: corre antes que cualquier otro envío del formulario
        document.addEventListener('submit', (e) => {
            const f = e.target;
            if (!f.dataset || !f.dataset.confirmar || f.dataset.confirmado === 'si') return;
            e.preventDefault();
            e.stopImmediatePropagation();
            abrirConfirmacion(f);
        }, true);

        dialogo.addEventListener('close', () => {
            const f = formularioPendiente.actual;
            formularioPendiente.actual = null;
            if (f && dialogo.returnValue === 'aceptar') {
                f.dataset.confirmado = 'si';
                f.requestSubmit();
            }
        });

        // Tocar fuera del cuadro lo cancela
        dialogo.addEventListener('click', (e) => {
            if (e.target === dialogo) dialogo.close('cancelar');
        });
    } else {
        // Navegador sin <dialog>: se usa la confirmación del navegador
        document.addEventListener('submit', (e) => {
            const mensaje = e.target.dataset ? e.target.dataset.confirmar : null;
            if (mensaje && !window.confirm(mensaje)) e.preventDefault();
        });
    }

    // <select data-autosubmit> envía su formulario al cambiar de opción
    document.addEventListener('change', (e) => {
        if (e.target.matches && e.target.matches('[data-autosubmit]') && e.target.form) {
            e.target.form.requestSubmit();
        }
    });

    // <button data-imprimir> abre el diálogo de impresión
    document.addEventListener('click', (e) => {
        if (e.target.closest('[data-imprimir]')) {
            window.print();
        }
    });

    // Hoja inferior (móvil): <button data-abrir-hoja="id"> la abre; [data-cerrar-hoja] la cierra
    document.addEventListener('click', (e) => {
        const abrir = e.target.closest('[data-abrir-hoja]');
        if (abrir) {
            const hoja = document.getElementById(abrir.dataset.abrirHoja);
            if (hoja) hoja.hidden = false;
            return;
        }
        if (e.target.closest('[data-cerrar-hoja]')) {
            const hoja = e.target.closest('.hoja');
            if (hoja) hoja.hidden = true;
        }
    });
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            document.querySelectorAll('.hoja').forEach((h) => { h.hidden = true; });
            document.querySelectorAll('details.menu-desplegable[open]').forEach((d) => { d.open = false; });
        }
    });

    // Los menús desplegables de la barra se cierran al hacer clic fuera
    document.addEventListener('click', (e) => {
        document.querySelectorAll('details.menu-desplegable[open]').forEach((d) => {
            if (!d.contains(e.target)) d.open = false;
        });
    });

    // Evita enviar dos veces un formulario al hacer doble clic
    document.addEventListener('submit', (e) => {
        if (e.defaultPrevented || e.target.method === 'dialog') return;
        const enviar = e.target.querySelector('button[type="submit"]:not([data-multiple])');
        if (enviar) {
            setTimeout(() => { enviar.disabled = true; }, 0);
        }
    });
})();
