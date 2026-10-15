// Pantalla de ingreso: mostrar/ocultar contraseña, aviso de mayúsculas y estado mientras entra.
(function () {
    'use strict';

    const formulario = document.getElementById('form-login');
    if (!formulario) return;

    const usuario = document.getElementById('nombreUsuario');
    const clave = document.getElementById('clave');
    const ver = document.getElementById('ver-clave');
    const avisoMayusculas = document.getElementById('aviso-mayusculas');
    const boton = document.getElementById('boton-entrar');

    // ---------- Mostrar u ocultar la contraseña ----------

    ver.addEventListener('click', () => {
        const visible = clave.type === 'password';
        clave.type = visible ? 'text' : 'password';
        ver.setAttribute('aria-pressed', String(visible));
        ver.setAttribute('aria-label', visible ? 'Ocultar contraseña' : 'Mostrar contraseña');
        ver.querySelector('[data-ojo]').hidden = visible;
        ver.querySelector('[data-ojo-cerrado]').hidden = !visible;
        clave.focus();
    });

    // ---------- Aviso de mayúsculas activadas ----------

    function revisarMayusculas(e) {
        if (typeof e.getModifierState !== 'function') return;
        avisoMayusculas.hidden = !e.getModifierState('CapsLock');
    }
    clave.addEventListener('keydown', revisarMayusculas);
    clave.addEventListener('keyup', revisarMayusculas);
    clave.addEventListener('blur', () => { avisoMayusculas.hidden = true; });

    // ---------- Enviar ----------

    formulario.addEventListener('submit', (e) => {
        if (!usuario.value.trim() || !clave.value) {
            e.preventDefault();
            (usuario.value.trim() ? clave : usuario).focus();
            return;
        }
        boton.querySelector('span').textContent = 'Ingresando…';
    });

    // Si se vuelve con el botón "atrás", el formulario no queda trabado
    window.addEventListener('pageshow', () => {
        boton.disabled = false;
        boton.querySelector('span').textContent = 'Ingresar';
    });

    // Tras un error, el cursor queda en la contraseña
    if (new URLSearchParams(location.search).has('error')) {
        clave.focus();
    }
})();
