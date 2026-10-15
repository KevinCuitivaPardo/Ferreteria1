// Copia Leaflet (la librería de mapas) desde node_modules a los archivos estáticos de la aplicación,
// para que los mapas no dependan de un CDN externo.
const fs = require('fs');
const path = require('path');

const origen = path.join('node_modules', 'leaflet', 'dist');
const destino = path.join('src', 'main', 'resources', 'static', 'vendor', 'leaflet');

fs.rmSync(destino, { recursive: true, force: true });
fs.mkdirSync(destino, { recursive: true });

for (const archivo of ['leaflet.css', 'leaflet.js']) {
    fs.copyFileSync(path.join(origen, archivo), path.join(destino, archivo));
}
fs.cpSync(path.join(origen, 'images'), path.join(destino, 'images'), { recursive: true });

console.log('Leaflet copiado a ' + destino);
