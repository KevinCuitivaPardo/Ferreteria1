# Ferretería · Sistema centralizado de sucursales

Inventario, ventas en el local y domicilios de varias ferreterías, con registro de eventos y mapas
(OpenStreetMap). Monolito modular en Java 21 (Spring Boot, Thymeleaf, JDBC) con PostgreSQL, arquitectura
hexagonal y una interfaz inspirada en las Human Interface Guidelines de Apple (Tailwind CSS).

## Tecnologías y APIs

| Capa | Qué se usa | Para qué |
|---|---|---|
| Lenguaje | Java 21, Maven | Todo el backend (migrado desde C#) |
| Framework web | Spring Boot 3.3.5 + Thymeleaf | Controladores MVC y páginas renderizadas en el servidor |
| Seguridad | Spring Security (BCrypt, login por formulario, CSRF) | Ingreso, roles `admin`, `vendedor`, `domiciliario` |
| Datos | Spring JDBC (`JdbcTemplate`) + PostgreSQL 16 | SQL explícito, sin ORM |
| Migraciones | Flyway | Esquema versionado (`V1`, `V2`, `V3`) y datos demo repetibles |
| Tiempo real | Spring WebSocket (`/ws`) | Alertas al domiciliario y ubicación en vivo para el admin |
| Frontend | Tailwind CSS 3.4, JavaScript sin dependencias | Interfaz estilo Apple, modo claro/oscuro, móvil |
| Mapas | Leaflet 1.9 + teselas de OpenStreetMap | Mapas de sucursales, pedidos y rutas |
| Geocodificación | Nominatim (OpenStreetMap), gratuita | Dirección → coordenadas, en el servidor |
| Rutas | OSRM (servidor público de demo), gratuita | Ruta en carro, calculada en el navegador |
| Pruebas | JUnit 5, Mockito, MockMvc, ArchUnit | Reglas de negocio, páginas, permisos, arquitectura |
| Infraestructura | Docker Compose | Base de datos PostgreSQL local |

Las APIs de OpenStreetMap no necesitan clave, pero piden uso moderado: la app respeta 1 consulta por segundo a Nominatim.
El servidor de OSRM es de demostración; para producción conviene uno propio.

## Cómo correrlo

**Requisitos:** JDK 21, Maven 3.9+, Docker Desktop. Node no hace falta instalarlo: Maven lo descarga solo.

```bash
docker compose up -d      # PostgreSQL en el puerto 5433 (el esquema lo crea la app al arrancar)
mvn spring-boot:run       # la primera vez descarga Node solo para compilar Tailwind y copiar Leaflet
```

Abre http://localhost:5107 · usuario `admin` · contraseña `admin123` (solo para desarrollo).

Usuarios de ejemplo (datos demo): `juan/juan123` (vendedor), `luis/luis123` y `pedro/pedro123` (domiciliarios).
Cámbialos o bórralos en producción.

Atajos útiles:

- Sin recompilar estilos: `mvn spring-boot:run -Dskip.npm -Dskip.installnodenpm`.
- Otro puerto: `PORT=8080`. Otra base: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.
- Probar el flujo completo: entra como `juan` (vendedor) y crea un pedido; entra como `admin` y asígnalo a `luis`;
  entra como `luis` en otro navegador, comparte la ubicación, comienza la entrega y termínala con una foto.
- Si el puerto 5433 está ocupado: `DB_PORT=5434 docker compose up -d` y arranca la app con el mismo `DB_PORT`.
- Si no aparecen estilos: ejecuta `npm install && npm run build` una vez.

- Empezar la base de cero: `docker compose down -v && docker compose up -d`.
- Datos de ejemplo (productos, una sucursal, un cliente): `src/main/resources/db/demo/`. En producción
  no se cargan con `FLYWAY_LOCATIONS=classpath:db/migration`.
- Variables de entorno (valores por defecto en `application.properties`): `DB_HOST`, `DB_PORT`, `DB_NAME`,
  `DB_USER`, `DB_PASSWORD`, `ADMIN_USUARIO`, `ADMIN_CLAVE`, `PORT`, `GEOCODIFICADOR_URL`,
  `GEOCODIFICADOR_PAIS`, `GEOCODIFICADOR_CONTACTO`. **Cámbialas en producción.**
- Si la tabla `usuarios` está vacía, al arrancar se crea el administrador con `ADMIN_USUARIO` / `ADMIN_CLAVE`.
- La base se versiona con Flyway (`src/main/resources/db/migration`). Una base creada antes de Flyway se toma como V1.

## Roles

| Rol | Qué puede hacer |
|---|---|
| **admin** | Todo, en todas las sucursales: elige la sucursal en la barra superior, crea sucursales y usuarios, define descuentos, asigna domiciliarios, ajusta ubicaciones, ve el mapa y el registro de eventos. |
| **vendedor** | En su sucursal: consulta el inventario, registra **ventas** (en el local) y **pedidos** (domicilios) con su descuento máximo. |
| **domiciliario** | Ve solo las entregas que le asignan, con la ruta desde la sucursal, el nombre de quien hizo el pedido, la dirección y los productos; las marca *En ruta*, *Entregado* o *No se pudo entregar*. |

## Lo que se registra

- **Venta (local):** nombre de quien compra, número de contacto, productos y cantidades, descuento, vendedor, sucursal y fecha.
  Descuenta el stock de la sucursal al guardar.
- **Pedido (domicilio):** lo mismo más la **dirección de entrega**, que se ubica en el mapa (OpenStreetMap / Nominatim).
  Descuenta el stock de la sucursal; si pasa a *No entregado* o *Cancelado*, el stock vuelve.
- **Inventario por sucursal:** el catálogo de productos es común; cada sucursal tiene sus existencias, movimientos y ajustes.
- **Registro de eventos:** ventas, descuentos, pedidos, cambios de estado, asignaciones, movimientos de inventario,
  y altas de productos, usuarios y sucursales, con quién lo hizo y cuándo (`/Eventos`).

- **Medio de pago y monto:** toda venta y pedido guarda el medio (efectivo, transferencia, tarjeta, Nequi, Daviplata) y
  el total. En efectivo se anota con cuánto pagó el cliente y se calcula el cambio.
- **Descuentos:** el admin los fija por vendedor y puede **quitarlos** (con confirmación) en `/Descuentos`; queda en el registro.

## Lógica y funcionamiento

### Sucursal activa
Cada usuario trabaja en una sucursal. El vendedor y el domiciliario usan la suya; el admin elige una en la barra superior
(se guarda en su sesión). Todo lo que se consulta o se escribe (inventario, ventas, pedidos) se filtra por esa sucursal.

### Inventario
- El **catálogo** de productos es único; el **stock** es por sucursal.
- Cada sucursal tiene su propia existencia por producto. Hay entradas, salidas y ajustes, y cada uno queda como movimiento.
- Una venta o pedido nunca deja el stock negativo: si no alcanza, se rechaza completo y no se guarda nada.

### Venta en el local
1. El vendedor agrega productos y cantidades, el nombre y teléfono del cliente, el medio de pago y la fecha.
2. Se calcula: subtotal → descuento del vendedor (porcentaje fijado por el admin) → **total**.
3. En efectivo se escribe con cuánto paga el cliente; debe ser ≥ total y se muestra el cambio.
4. En **una sola transacción** se guarda la venta, se descuenta el stock y se registra el evento.

### Pedido (domicilio)
1. Igual que la venta, más la **dirección de entrega**.
2. Antes de guardar se geocodifica la dirección (Nominatim). Si no se encuentra, el pedido se guarda sin punto en el mapa
   y el admin puede ajustarlo a mano.
3. Se descuenta el stock y se avisa al admin en tiempo real de que hay un pedido nuevo.
4. **Estados:** `Pendiente` → `En ruta` → `Entregado`, o `No entregado` / `Cancelado`. Si un pedido pasa a *No entregado* o
   *Cancelado*, el stock se devuelve; si se reactiva, se vuelve a descontar.

### Asignación y entrega
1. El admin asigna el pedido a un domiciliario (también puede reasignarlo).
2. Al domiciliario le llega una alerta en pantalla (banner, sonido y vibración) y su cola `/Entregas` se actualiza sola.
3. Su cola se ordena por hora de asignación. **Solo puede tener una entrega en ruta** (lo exige también la base de datos);
   comienza una, y recién al terminarla puede comenzar la siguiente.
4. Al comenzar, el mapa muestra la ruta desde su posición hasta el cliente y el pedido pasa a *En ruta*.
5. Al terminar elige *Entregado* (con foto obligatoria) o *No se pudo entregar* (con una nota opcional).
6. El admin ve la foto, la nota y las horas de asignación, inicio y fin en el detalle del pedido.

### Ubicación en vivo
El domiciliario activa *Compartir ubicación*; su navegador envía la posición por WebSocket cada pocos segundos
(el servidor limita a 1 mensaje cada 1,5 s). El servidor la guarda en memoria y la reenvía solo a los admin conectados, que
ven al conductor moverse en `/Pedidos/Mapa`. Las posiciones tienen validez de 30 minutos y no se guardan en base de datos.
Si la conexión se cae, el navegador reconecta solo con espera creciente.

### Descuentos
El admin define un porcentaje por vendedor. Se aplica al total de cada pedido o venta **nueva**; lo ya guardado no cambia.
Se puede quitar (vuelve a 0 %) y cada cambio queda en el registro de eventos.

### Registro de eventos
Cada acción importante escribe un evento (tipo, texto, usuario, sucursal, fecha) **dentro de la misma transacción** que el
cambio, así que nunca hay un cambio sin evento ni un evento sin cambio. El admin los filtra por tipo en `/Eventos`.

### Seguridad
- Contraseñas con BCrypt; formularios con token CSRF; salir solo por POST.
- Cada ruta exige un rol (ver tabla de rutas). Un vendedor no ve otras sucursales ni el panel de admin.
- Las fotos se sirven solo al admin, al vendedor que creó el pedido y al domiciliario asignado; se validan los primeros
  bytes del archivo (no solo la extensión) y el tamaño.
- El WebSocket usa la cookie de sesión y acepta solo el mismo origen.

## Rutas principales

| Ruta | Quién | Qué hace |
|---|---|---|
| `/Login`, `/Salir` | todos | Ingreso y cierre de sesión (confirmado) |
| `/Productos`, `/Movimientos/{id}` | admin, vendedor | Inventario y movimientos de la sucursal activa |
| `/Ventas`, `/Ventas/Nueva` | admin, vendedor | Historial y registro de ventas |
| `/Pedidos/Lista`, `/Pedidos/Nuevo`, `/Pedidos/Detalle/{id}` | admin, vendedor | Pedidos, detalle, estado, foto de entrega |
| `/Pedidos/Mapa` | admin | Mapa de pedidos y conductores en vivo |
| `/Entregas`, `/Entregas/{id}` | domiciliario | Cola, comenzar y terminar con foto |
| `/Fotos/{pedidoId}/{fotoId}` | admin, dueño, asignado | Foto de entrega |
| `/Sucursales`, `/Usuarios`, `/Descuentos`, `/Eventos` | admin | Administración (alta de sucursales y usuarios, descuentos, registro) |
| `/Seguimiento/posiciones` | admin | Posiciones actuales de los conductores (JSON) |
| `/api/geocodificar` | admin, vendedor | Dirección → coordenadas (JSON) |
| `/ws` | sesión iniciada | WebSocket de alertas y ubicación |

Mensajes del WebSocket (JSON): el domiciliario envía `{"tipo":"posicion","latitud":..,"longitud":..,"precision":..}`;
el servidor envía `{"tipo":"posicion",...}` a los admin y `{"tipo":"aviso",...}` (asignación, cola, pedido nuevo) a quien corresponda.

## Base de datos

Tablas principales: `sucursales`, `usuarios`, `clientes`, `productos`, `inventario_sucursal`, `movimientos_inventario`,
`ventas` y `venta_items`, `pedidos` y `pedido_items`, `pedido_fotos`, `eventos`. Se crean con Flyway al arrancar; los datos demo
(`db/demo/R__datos_demo.sql`) se vuelven a aplicar solo si cambian y no se cargan con `FLYWAY_LOCATIONS=classpath:db/migration`.

## Entregas y tiempo real

- **Cola del domiciliario (`/Entregas`):** *En ruta ahora*, *En espera* (numeradas) y *Terminadas*. Comienza una a la vez;
  al comenzar ve la ruta en el mapa. Para terminar es **obligatoria una foto** de la entrega (JPEG/PNG/WEBP, hasta 8 MB),
  que el admin revisa en el detalle del pedido. Las fotos se guardan en `datos/fotos` (`FOTOS_DIRECTORIO`).
- **WebSocket (`/ws`):** el domiciliario recibe alertas al asignarle un pedido y el admin ve su ubicación en vivo en el mapa.
  Las posiciones viven solo en memoria. Las alertas llegan únicamente con la página abierta (no hay notificaciones push).
  En celulares el GPS exige **HTTPS** en producción.
- **Sesión:** cerrar sesión pide confirmación y solo funciona por POST (`/Salir`). El ingreso tiene mostrar/ocultar contraseña
  y aviso de mayúsculas.

## Mapas

- **Sucursales:** al crearlas se busca la dirección o se toca el mapa para marcar el punto exacto.
- **Mapa de domicilios (admin):** todos los pedidos ubicados, coloreados por estado, con las sucursales.
- **Domiciliario / detalle del pedido:** ruta en carro desde la sucursal hasta la entrega (OSRM, también gratuito).
- **Precisión:** OpenStreetMap casi no tiene números de casa en Colombia. Una dirección como `Carrera 7 # 72-41, Bogotá`
  se busca por su calle numerada (`Calle 72, Bogotá`), así que el punto es **aproximado**; el administrador puede
  ajustarlo a mano en el detalle del pedido ("Ajustar el punto de entrega"). Si no se encuentra, el pedido se guarda sin
  ubicación. Incluye la ciudad en la dirección. Los servicios gratuitos piden uso moderado (Nominatim: 1 consulta por
  segundo, ya respetado); para mucho volumen conviene un servicio propio o de pago (`GEOCODIFICADOR_URL`).

## Arquitectura (hexagonal, por módulos)

Cada módulo de negocio tiene tres capas y las dependencias siempre apuntan hacia adentro:

```
infrastructure  ──►  application  ──►  domain
(adaptadores)        (casos de uso      (reglas y datos
                      y puertos)         del negocio)
```

```
src/main/java/com/ferreteria
├── inventario/       productos y stock por sucursal, movimientos
├── usuarios/         usuarios, roles (admin, vendedor, domiciliario) y descuentos
├── sucursales/       sucursales con su ubicación; sucursal activa del administrador
├── geolocalizacion/  direcciones → coordenadas (adaptador de OpenStreetMap)
├── eventos/          consulta del registro de eventos
├── ventas/           ventas en el local
├── pedidos/          domicilios, asignación, entregas y ubicación
├── shared/           lo común: NegocioException, eventos, totales, sesión, formatos
└── config/           seguridad por rol, inicio y arranque
```

| Carpeta (dentro de cada módulo) | Qué contiene |
|---|---|
| `domain/` | Registros y reglas del negocio. Sin Spring ni base de datos. |
| `application/port/in/` | Lo que el módulo **ofrece** (casos de uso: `RegistrarVenta`, `CrearPedido`, `MoverStock`...). |
| `application/port/out/` | Lo que el módulo **necesita** (`PedidoRepository`, `CatalogoProductos`, `UbicadorDeDirecciones`...). |
| `application/*Service` | Implementa los puertos de entrada usando los de salida. |
| `infrastructure/in/web/` | Controladores y formularios (adaptadores de entrada). |
| `infrastructure/out/persistence/` | SQL con JdbcTemplate (adaptadores de salida). |
| `infrastructure/out/<módulo>/` | Conexión con otro módulo, solo a través de sus puertos de entrada. |

`ventas` y `pedidos` dependen de `inventario`, `usuarios` y `geolocalizacion` **solo** mediante adaptadores que usan sus
puertos de entrada; nunca al revés. `ArquitecturaTest` hace fallar la construcción si alguien rompe estas reglas.

Para **agregar una función**: define el caso de uso en `port/in`, impleméntalo en `application`, y conéctalo con un
controlador y/o un repositorio nuevos. Para **agregar un módulo** (por ejemplo `compras/`), crea la misma estructura.
Para **cambiar el proveedor de mapas o la base de datos** solo se tocan los adaptadores.

## Interfaz

- **Tailwind CSS** con colores semánticos (`fondo`, `tarjeta`, `etiqueta`, `accent`...) que cambian solos en modo oscuro.
- Estilo Apple: tipografía del sistema, títulos grandes, barra superior translúcida, barra de pestañas y hoja "Más" en móvil,
  tarjetas redondeadas, botones tintados, control segmentado y objetivos táctiles de 44 px.
- Estilos en `src/main/frontend/` (`app.css`, `tailwind.config.js`); Leaflet (mapas) se copia desde npm a
  `static/vendor/`. Maven lo prepara al construir; ambos se generan y no se guardan en git.
  Para editar estilos en vivo: `npm install` y `npm run dev`.
- Plantillas en `src/main/resources/templates/`; JavaScript sin dependencias en `static/js/`.

## Pruebas

```bash
mvn test
```

- `ArquitecturaTest`: reglas de la arquitectura.
- `PedidoServiceTest`, `EntregaServiceTest`, `VentaServiceTest`, `UsuarioServiceTest`, `TiempoRealServiceTest`: casos de uso con puertos simulados (sin Spring, base de datos ni internet).
- `TiempoRealWebSocketTest`: ingreso real y WebSocket real contra la app levantada.
- `PaginasTest`: rutas, permisos por rol y plantillas con los puertos de entrada simulados.
- `DireccionesColombianasTest`: normalización de direcciones.

## Versión anterior

El proyecto original en C# / ASP.NET Razor Pages quedó en `legacy-dotnet/` solo como referencia; se puede borrar.
