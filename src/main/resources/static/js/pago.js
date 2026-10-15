// Bloque de pago: muestra el total a pagar y calcula el cambio cuando se paga en efectivo.
// El total llega por el evento "totales-cambiaron" que lanza lineas.js.
(function () {
    'use strict';

    const medio = document.getElementById('medioPago');
    const bloque = document.getElementById('bloque-recibido');
    const recibido = document.getElementById('montoRecibido');
    const totalEl = document.getElementById('pago-total');
    const cambioEl = document.getElementById('pago-cambio');
    if (!medio || !bloque || !recibido || !totalEl || !cambioEl) return;

    const formato = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
    let total = window.FerreteriaTotal || 0;

    function actualizar() {
        const efectivo = medio.value === 'efectivo';
        bloque.hidden = !efectivo;
        if (!efectivo) recibido.value = '';

        totalEl.textContent = formato.format(total);
        cambioEl.classList.remove('text-peligro-texto', 'text-exito-texto');

        const dinero = Number(recibido.value || 0);
        if (efectivo && dinero > 0) {
            if (dinero < total) {
                cambioEl.textContent = 'Faltan ' + formato.format(total - dinero);
                cambioEl.classList.add('text-peligro-texto');
            } else {
                cambioEl.textContent = 'Cambio: ' + formato.format(dinero - total);
                cambioEl.classList.add('text-exito-texto');
            }
        } else {
            cambioEl.textContent = '';
        }
    }

    document.addEventListener('totales-cambiaron', (e) => {
        total = e.detail.total;
        actualizar();
    });
    medio.addEventListener('change', actualizar);
    recibido.addEventListener('input', actualizar);
    actualizar();
})();
