// Crear usuario: el administrador no pertenece a una sucursal, así que se deshabilita ese campo.
(function () {
    'use strict';

    const rol = document.getElementById('rol');
    const sucursal = document.getElementById('sucursalId');
    if (!rol || !sucursal) return;

    function ajustar() {
        sucursal.disabled = rol.value === 'admin';
    }

    rol.addEventListener('change', ajustar);
    ajustar();
})();
