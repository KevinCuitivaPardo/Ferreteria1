/** @type {import('tailwindcss').Config} */

// Colores semánticos al estilo de las Human Interface Guidelines de Apple:
// no se nombran por tono sino por función, y cambian solos en modo oscuro (ver app.css).
const color = (variable) => `rgb(var(${variable}) / <alpha-value>)`;

module.exports = {
  content: [
    './src/main/resources/templates/**/*.html',
    './src/main/resources/static/js/**/*.js',
  ],
  theme: {
    extend: {
      colors: {
        fondo: color('--fondo'),             // systemGroupedBackground
        tarjeta: color('--tarjeta'),         // secondarySystemGroupedBackground
        etiqueta: color('--etiqueta'),       // label
        secundario: color('--secundario'),   // secondaryLabel
        terciario: color('--terciario'),     // tertiaryLabel
        separador: color('--separador'),     // separator
        relleno: color('--relleno'),         // systemFill
        accent: color('--accent'),           // systemBlue
        peligro: color('--peligro'),         // systemRed
        'peligro-texto': color('--peligro-texto'),
        exito: color('--exito'),             // systemGreen
        'exito-texto': color('--exito-texto'),
        aviso: color('--aviso'),             // systemOrange
        'aviso-texto': color('--aviso-texto'),
        indigo: color('--indigo'),
        morado: color('--morado'),
        turquesa: color('--turquesa'),
      },
      fontFamily: {
        // San Francisco en dispositivos Apple; la mejor fuente del sistema en el resto
        sans: [
          '-apple-system', 'BlinkMacSystemFont', '"SF Pro Text"', '"SF Pro Display"',
          '"Segoe UI Variable Text"', '"Segoe UI"', 'system-ui', 'Roboto', 'Helvetica', 'Arial', 'sans-serif',
        ],
        mono: ['ui-monospace', '"SF Mono"', 'SFMono-Regular', 'Menlo', 'Consolas', 'monospace'],
      },
      borderRadius: {
        ios: '10px',
      },
      boxShadow: {
        tarjeta: '0 0 0 0.5px rgb(var(--separador) / 0.18), 0 1px 3px rgb(0 0 0 / 0.04)',
        elevada: '0 10px 30px rgb(0 0 0 / 0.14), 0 0 0 0.5px rgb(var(--separador) / 0.2)',
      },
      transitionTimingFunction: {
        ios: 'cubic-bezier(0.32, 0.72, 0, 1)',
      },
    },
  },
  plugins: [require('@tailwindcss/forms')],
};
