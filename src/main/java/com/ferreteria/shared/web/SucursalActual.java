package com.ferreteria.shared.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Parámetro de controlador (tipo {@code int}) con la sucursal en la que se está trabajando:
 * la propia del vendedor o domiciliario, o la que el administrador eligió en la barra superior.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SucursalActual {
}
