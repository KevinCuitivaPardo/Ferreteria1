// Mapa del administrador: domicilios ubicados (por estado), sucursales y domiciliarios en vivo.
(function () {
    'use strict';

    const elemento = document.getElementById('mapa-pedidos');
    if (!elemento || !window.L) return;

    const SIN_SENAL_MS = 90 * 1000;

    const mapa = FerreteriaMapa.crear(elemento, { zoom: 12 });
    const leyenda = document.getElementById('conteo-pedidos');
    const filtro = document.getElementById('filtro-estado-mapa');
    const listaConductores = document.getElementById('lista-conductores');
    const sinConductores = document.getElementById('sin-conductores');
    const estadoConexion = document.getElementById('estado-conexion');
    const capaPedidos = L.layerGroup().addTo(mapa);
    const capaSucursales = L.layerGroup().addTo(mapa);

    const conductores = new Map(); // usuarioId -> { datos, marcador, recibido }
    let pedidos = [];
    let ajustado = false;

    const ETIQUETAS = {
        pendiente: 'Pendiente', alistado: 'Alistado', en_ruta: 'En ruta',
        entregado: 'Entregado', no_entregado: 'No entregado', cancelado: 'Cancelado',
    };

    // ---------- Pedidos ----------

    function dibujarPedidos() {
        capaPedidos.clearLayers();
        const estado = filtro ? filtro.value : '';
        const visibles = pedidos.filter((p) => !estado || p.estado === estado);

        visibles.forEach((p) => {
            L.marker([p.latitud, p.longitud], { icon: FerreteriaMapa.pin(FerreteriaMapa.colorDeEstado(p.estado)) })
                .addTo(capaPedidos)
                .bindPopup(
                    '<strong>' + FerreteriaMapa.escapar(p.numero) + '</strong> · ' + ETIQUETAS[p.estado] + '<br>'
                    + FerreteriaMapa.escapar(p.cliente) + '<br>'
                    + FerreteriaMapa.escapar(p.direccion) + '<br>'
                    + '<span style="opacity:.7">' + FerreteriaMapa.escapar(p.sucursal)
                    + (p.domiciliario ? ' · ' + FerreteriaMapa.escapar(p.domiciliario) : ' · sin domiciliario')
                    + '</span><br><a href="/Pedidos/Detalle/' + p.id + '">Ver pedido</a>');
        });

        if (leyenda) {
            leyenda.textContent = visibles.length === 1 ? '1 domicilio en el mapa' : visibles.length + ' domicilios en el mapa';
        }
        return visibles.map((p) => [p.latitud, p.longitud]);
    }

    async function cargarPedidos() {
        try {
            const respuesta = await fetch('/Pedidos/Mapa/datos');
            pedidos = respuesta.ok ? await respuesta.json() : [];
            const puntos = dibujarPedidos();
            pintarLista();

            if (!ajustado) {
                const sucursales = await cargarSucursales();
                const todos = puntos.concat(sucursales);
                if (todos.length) {
                    mapa.fitBounds(L.latLngBounds(todos), { padding: [40, 40], maxZoom: 15 });
                    ajustado = true;
                }
            }
        } catch (error) {
            if (leyenda) leyenda.textContent = 'No se pudieron cargar los domicilios.';
        }
    }

    async function cargarSucursales() {
        const respuesta = await fetch('/Sucursales/datos');
        const lugares = respuesta.ok ? await respuesta.json() : [];
        capaSucursales.clearLayers();
        return lugares.map((s) => {
            const punto = [s.latitud, s.longitud];
            L.marker(punto, { icon: FerreteriaMapa.pin('#1c1c1e', true) }).addTo(capaSucursales)
                .bindPopup('<strong>' + FerreteriaMapa.escapar(s.nombre) + '</strong><br>' + FerreteriaMapa.escapar(s.direccion));
            return punto;
        });
    }

    // ---------- Domiciliarios en vivo ----------

    function icono(nombre, sinSenal) {
        return L.divIcon({
            className: 'pin-contenedor',
            html: '<span class="conductor' + (sinSenal ? ' sin-senal' : '') + '">' + FerreteriaMapa.escapar(nombre) + '</span>',
            iconSize: [0, 0],
        });
    }

    function actualizarConductor(datos) {
        const punto = [datos.latitud, datos.longitud];
        let c = conductores.get(datos.usuarioId);
        if (!c) {
            c = { marcador: L.marker(punto, { icon: icono(datos.nombre, false), zIndexOffset: 900 }).addTo(mapa) };
            conductores.set(datos.usuarioId, c);
        } else {
            c.marcador.setLatLng(punto);
        }
        c.datos = datos;
        c.recibido = datos.actualizada ? Date.parse(datos.actualizada) : Date.now();
        pintarLista();
    }

    function hace(ms) {
        const s = Math.round(ms / 1000);
        if (s < 5) return 'ahora';
        if (s < 60) return 'hace ' + s + ' s';
        return 'hace ' + Math.round(s / 60) + ' min';
    }

    function pedidoEnRuta(nombre) {
        return pedidos.find((p) => p.estado === 'en_ruta' && p.domiciliario === nombre);
    }

    function pintarLista() {
        if (!listaConductores) return;
        listaConductores.replaceChildren();
        if (sinConductores) sinConductores.hidden = conductores.size > 0;

        [...conductores.values()]
            .sort((a, b) => a.datos.nombre.localeCompare(b.datos.nombre))
            .forEach((c) => {
                const edad = Date.now() - c.recibido;
                const sinSenal = edad > SIN_SENAL_MS;
                const pedido = pedidoEnRuta(c.datos.nombre);

                c.marcador.setIcon(icono(c.datos.nombre, sinSenal));

                const li = document.createElement('li');
                const boton = document.createElement('button');
                boton.type = 'button';
                boton.className = 'flex w-full items-center justify-between gap-3 rounded-xl px-3 py-2.5 text-left hover:bg-relleno/10';
                boton.addEventListener('click', () => mapa.setView(c.marcador.getLatLng(), Math.max(mapa.getZoom(), 15)));

                const izquierda = document.createElement('span');
                izquierda.className = 'min-w-0';
                const nombre = document.createElement('strong');
                nombre.className = 'block truncate';
                nombre.textContent = c.datos.nombre;
                const detalle = document.createElement('span');
                detalle.className = 'block truncate text-[13px] text-secundario';
                detalle.textContent = pedido ? 'En ruta · ' + pedido.numero + ' · ' + pedido.cliente : 'Sin entrega en ruta';
                izquierda.append(nombre, detalle);

                const derecha = document.createElement('span');
                derecha.className = 'estado shrink-0 ' + (sinSenal ? 'estado-pendiente' : 'estado-entregado');
                derecha.textContent = sinSenal ? 'Sin señal · ' + hace(edad) : hace(edad);

                boton.append(izquierda, derecha);
                li.appendChild(boton);
                listaConductores.appendChild(li);
            });
    }

    // ---------- Tiempo real ----------

    FerreteriaTiempoReal.escuchar((m) => {
        if (m.tipo === 'posicion') {
            actualizarConductor(m.datos);
        } else if (m.tipo === 'conexion' && estadoConexion) {
            estadoConexion.textContent = m.datos.conectado ? 'En vivo' : 'Reconectando…';
            if (m.datos.conectado) cargarPosiciones();
        } else if (m.tipo === 'aviso') {
            cargarPedidos(); // un pedido nuevo o un cambio de estado: se refresca el mapa
        }
    });

    if (estadoConexion && FerreteriaTiempoReal.estaConectado()) estadoConexion.textContent = 'En vivo';

    async function cargarPosiciones() {
        try {
            const respuesta = await fetch('/Seguimiento/posiciones');
            if (!respuesta.ok) return;
            (await respuesta.json()).forEach((p) => actualizarConductor({ ...p }));
        } catch (error) { /* se verá con los mensajes en vivo */ }
    }

    if (filtro) filtro.addEventListener('change', dibujarPedidos);
    setInterval(pintarLista, 5000);      // refresca los "hace N s" y las marcas de sin señal
    setInterval(cargarPedidos, 60000);   // por si se escapa algún aviso

    cargarPedidos();
    cargarPosiciones();
})();
