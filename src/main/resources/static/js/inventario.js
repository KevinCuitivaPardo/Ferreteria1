// Pantalla "Inventario": buscador con sugerencias mientras se escribe.
(function () {
    'use strict';

    const input = document.getElementById('buscar');
    const lista = document.getElementById('sugerencias');
    const buscador = document.getElementById('buscador');
    if (!input || !lista || !buscador) return;

    const esAdmin = buscador.dataset.admin === 'true';
    const pesos = (n) => '$ ' + Number(n).toLocaleString('es-CO', { maximumFractionDigits: 0 });
    let temporizador;
    let ultimaBusqueda = 0;

    input.addEventListener('input', () => {
        clearTimeout(temporizador);
        temporizador = setTimeout(buscar, 250);
    });

    input.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') lista.hidden = true;
        if (e.key === 'ArrowDown' && !lista.hidden) {
            const primero = lista.querySelector('a');
            if (primero) { e.preventDefault(); primero.focus(); }
        }
    });

    lista.addEventListener('keydown', (e) => {
        const enlaces = [...lista.querySelectorAll('a')];
        const i = enlaces.indexOf(document.activeElement);
        if (e.key === 'ArrowDown' && i < enlaces.length - 1) { e.preventDefault(); enlaces[i + 1].focus(); }
        if (e.key === 'ArrowUp') { e.preventDefault(); (i > 0 ? enlaces[i - 1] : input).focus(); }
        if (e.key === 'Escape') { lista.hidden = true; input.focus(); }
    });

    document.addEventListener('click', (e) => {
        if (!e.target.closest('#buscador')) lista.hidden = true;
    });

    async function buscar() {
        const q = input.value.trim();
        if (q.length === 0) {
            lista.hidden = true;
            return;
        }

        const turno = ++ultimaBusqueda;
        try {
            const resp = await fetch(`/Productos/buscar?q=${encodeURIComponent(q)}`);
            if (!resp.ok) return;
            const datos = await resp.json();
            if (turno === ultimaBusqueda) mostrar(datos); // ignora respuestas viejas
        } catch (error) {
            lista.hidden = true;
        }
    }

    function mostrar(datos) {
        lista.replaceChildren();

        if (datos.length === 0) {
            const li = document.createElement('li');
            li.textContent = 'Sin resultados';
            li.className = 'p-4 text-secundario';
            lista.appendChild(li);
        }

        datos.forEach((p) => {
            const li = document.createElement('li');
            const a = document.createElement('a');
            // El administrador va a editar; el vendedor, a los movimientos del producto.
            a.href = esAdmin ? `/Editar/${p.id}` : `/Movimientos/${p.id}`;

            const titulo = document.createElement('strong');
            titulo.textContent = `${p.codigo}  ·  ${p.nombre} ${p.medida ?? ''}`;

            const detalle = document.createElement('div');
            detalle.className = p.stockBajo ? 'text-sm text-peligro-texto' : 'text-sm text-secundario';
            detalle.textContent = `Venta: ${pesos(p.precioVenta)}  ·  Stock: ${p.stock}` +
                                  (p.stockBajo ? '  ·  ⚠ STOCK BAJO' : '');

            a.append(titulo, detalle);
            li.appendChild(a);
            lista.appendChild(li);
        });

        lista.hidden = false;
    }
})();
