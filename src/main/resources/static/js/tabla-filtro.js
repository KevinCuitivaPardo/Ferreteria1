// Filtro de tablas sin recargar. La tabla tiene id "tabla-filtrable" y sus filas el atributo data-fila;
// opcionalmente un <select id="filtro-estado"> filtra por data-estado.
(function () {
    'use strict';

    const texto = document.getElementById('filtro-texto');
    const estado = document.getElementById('filtro-estado');
    const tabla = document.getElementById('tabla-filtrable');
    const vacio = document.getElementById('sin-resultados');
    if (!texto || !tabla) return;

    const filas = [...tabla.querySelectorAll('tbody tr[data-fila]')];

    function filtrar() {
        const q = texto.value.trim().toLowerCase();
        const e = estado ? estado.value : '';
        let visibles = 0;

        filas.forEach((fila) => {
            const coincide = (!e || fila.dataset.estado === e) &&
                (!q || fila.textContent.toLowerCase().includes(q));
            fila.hidden = !coincide;
            if (coincide) visibles++;
        });

        if (vacio) vacio.hidden = visibles > 0;
    }

    texto.addEventListener('input', filtrar);
    if (estado) estado.addEventListener('change', filtrar);
})();
