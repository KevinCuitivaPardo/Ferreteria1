// Líneas de productos y totales en vivo. Lo usan "Nuevo pedido" y "Nueva venta".
(function () {
    'use strict';

    const LINEAS_VISIBLES_AL_INICIO = 3;

    const contenedor = document.getElementById('lineas');
    if (!contenedor) return;

    const formato = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
    const descuento = document.getElementById('descuento');
    const filas = [...contenedor.querySelectorAll('.linea')];
    const botonAgregar = document.getElementById('agregar-linea');

    // ---------- Líneas: se muestran las que tienen datos y unas pocas vacías ----------

    const tieneDatos = (fila) =>
        fila.querySelector('.linea-producto').value !== '0' || fila.querySelector('.linea-cantidad').value !== '';

    function mostrarLineasIniciales() {
        let ultimaConDatos = 0;
        filas.forEach((fila, i) => { if (tieneDatos(fila)) ultimaConDatos = i + 1; });

        const visibles = Math.max(LINEAS_VISIBLES_AL_INICIO, ultimaConDatos);
        filas.forEach((fila, i) => { fila.hidden = i >= visibles; });
        actualizarBotonAgregar();
    }

    function actualizarBotonAgregar() {
        botonAgregar.hidden = !filas.some((f) => f.hidden);
    }

    botonAgregar.addEventListener('click', () => {
        const siguiente = filas.find((f) => f.hidden);
        if (siguiente) {
            siguiente.hidden = false;
            siguiente.querySelector('.linea-producto').focus();
        }
        actualizarBotonAgregar();
    });

    // ---------- Totales ----------

    function recalcular() {
        let subtotal = 0;

        filas.forEach((fila) => {
            const select = fila.querySelector('.linea-producto');
            const cantidadInput = fila.querySelector('.linea-cantidad');
            const opcion = select.options[select.selectedIndex];
            const precio = Number(opcion && opcion.dataset.precio ? opcion.dataset.precio : 0);
            const stock = Number(opcion && opcion.dataset.stock ? opcion.dataset.stock : 0);
            const cantidad = Number(cantidadInput.value || 0);
            const linea = precio * cantidad;

            // Avisa si la cantidad supera el stock (el servidor lo valida igual al guardar)
            cantidadInput.setCustomValidity(stock && cantidad > stock ? `Solo hay ${stock} en stock.` : '');
            cantidadInput.classList.toggle('border-peligro', Boolean(stock && cantidad > stock));

            fila.querySelector('.linea-precio').textContent = precio > 0 ? formato.format(precio) : '—';
            fila.querySelector('.linea-subtotal').textContent = linea > 0 ? formato.format(linea) : '—';
            subtotal += linea;
        });

        const pct = Number(descuento ? descuento.value : 0);
        const montoDescuento = subtotal * pct / 100;
        const total = Math.round(subtotal - montoDescuento);

        document.getElementById('etiqueta-descuento').textContent = `Descuento (${pct}%)`;
        document.getElementById('resumen-subtotal').textContent = formato.format(subtotal);
        document.getElementById('resumen-descuento').textContent = '− ' + formato.format(montoDescuento);
        document.getElementById('resumen-total').textContent = formato.format(total);

        // El bloque de pago (pago.js) usa este total para calcular el cambio
        window.FerreteriaTotal = total;
        document.dispatchEvent(new CustomEvent('totales-cambiaron', { detail: { total } }));
    }

    contenedor.addEventListener('input', recalcular);
    contenedor.addEventListener('change', recalcular);
    if (descuento) descuento.addEventListener('change', recalcular);

    mostrarLineasIniciales();
    recalcular();
})();
