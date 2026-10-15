// "Nuevo pedido": datos del cliente por celular y vista previa de la dirección en el mapa.
(function () {
    'use strict';

    const celular = document.getElementById('celular1');
    const direccion = document.getElementById('direccion');
    if (!celular || !direccion) return;

    const estadoCliente = document.getElementById('estado-cliente');
    const estadoDireccion = document.getElementById('estado-direccion');

    // ---------- Cliente por celular ----------

    celular.addEventListener('blur', async (e) => {
        const cel = e.target.value.trim();

        if (!cel) {
            estadoCliente.textContent = '';
            return;
        }

        estadoCliente.textContent = 'Buscando cliente...';

        try {
            const respuesta = await fetch('/Pedidos/Nuevo/cliente?cel=' + encodeURIComponent(cel));
            const datos = await respuesta.json();

            if (datos.encontrado) {
                document.getElementById('nombreFerreteria').value = datos.nombreFerreteria || '';
                document.getElementById('nombreCliente').value = datos.nombreCliente || '';
                direccion.value = datos.direccion || '';
                document.getElementById('ubicacion').value = datos.ubicacion || '';
                document.getElementById('horario').value = datos.horario || '';
                document.getElementById('celular2').value = datos.celular2 || '';
                estadoCliente.textContent = 'Cliente encontrado. Revisa sus datos antes de guardar.';
                comprobarDireccion();
            } else {
                estadoCliente.textContent = 'Cliente nuevo. Escribe sus datos.';
            }
        } catch (error) {
            estadoCliente.textContent = 'No se pudo buscar el cliente. Puedes escribir los datos a mano.';
        }
    });

    // ---------- Dirección de entrega: se comprueba que exista en el mapa ----------

    let ultimaConsulta = 0;

    async function comprobarDireccion() {
        const texto = direccion.value.trim();
        if (texto.length < 6) {
            estadoDireccion.textContent = '';
            return;
        }

        const turno = ++ultimaConsulta;
        estadoDireccion.textContent = 'Buscando la dirección en el mapa...';
        try {
            const respuesta = await fetch('/api/geocodificar?q=' + encodeURIComponent(texto));
            const datos = await respuesta.json();
            if (turno !== ultimaConsulta) return; // llegó una respuesta vieja

            estadoDireccion.textContent = datos.encontrado
                ? '📍 Ubicada: ' + datos.descripcion
                : 'No se encontró en el mapa. Agrega barrio o ciudad; el pedido se guarda igual, sin ubicación.';
        } catch (error) {
            estadoDireccion.textContent = '';
        }
    }

    direccion.addEventListener('blur', comprobarDireccion);
})();
