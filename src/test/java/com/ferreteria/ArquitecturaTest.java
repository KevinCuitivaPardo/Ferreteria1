package com.ferreteria;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Reglas de la arquitectura hexagonal. Si alguien las rompe, la construcción falla.
 *
 * <pre>
 *   domain  &lt;-  application (puertos y casos de uso)  &lt;-  infrastructure (adaptadores)
 * </pre>
 *
 * Los módulos de negocio son: inventario, usuarios, sucursales, geolocalizacion, eventos, tiemporeal (base)
 * y ventas, pedidos (los que dependen de los demás).
 */
@AnalyzeClasses(packages = "com.ferreteria", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    // ---------- Capas dentro de cada módulo ----------

    @ArchTest
    static final ArchRule el_dominio_no_conoce_otras_capas =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..")
                    .because("el dominio solo contiene reglas y datos del negocio");

    @ArchTest
    static final ArchRule el_dominio_no_depende_de_frameworks =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..", "jakarta..", "java.sql..")
                    .because("el dominio debe poder probarse sin Spring ni base de datos");

    @ArchTest
    static final ArchRule la_aplicacion_no_conoce_los_adaptadores =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("la aplicación solo habla con puertos, nunca con adaptadores");

    @ArchTest
    static final ArchRule la_aplicacion_no_usa_jdbc_ni_web =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.jdbc..", "org.springframework.web..",
                            "org.springframework.dao..", "org.springframework.security..", "javax.sql..",
                            "org.springframework.http..")
                    .because("los casos de uso no saben cómo se guarda ni cómo se muestra nada");

    // ---------- Entre módulos ----------

    @ArchTest
    static final ArchRule el_nucleo_de_ventas_y_pedidos_no_conoce_otros_modulos =
            noClasses().that().resideInAnyPackage(
                            "com.ferreteria.pedidos.domain..", "com.ferreteria.pedidos.application..",
                            "com.ferreteria.ventas.domain..", "com.ferreteria.ventas.application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.ferreteria.inventario..", "com.ferreteria.usuarios..",
                            "com.ferreteria.sucursales..", "com.ferreteria.geolocalizacion..",
                            "com.ferreteria.eventos..", "com.ferreteria.tiemporeal..")
                    .because("definen lo que necesitan como puertos de salida; "
                            + "sus adaptadores se conectan con los otros módulos");

    @ArchTest
    static final ArchRule los_adaptadores_solo_usan_puertos_de_entrada_de_otros_modulos =
            noClasses().that().resideInAnyPackage(
                            "com.ferreteria.pedidos.infrastructure..", "com.ferreteria.ventas.infrastructure..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.ferreteria.inventario.infrastructure..", "com.ferreteria.inventario.application.port.out..",
                            "com.ferreteria.inventario.application",
                            "com.ferreteria.usuarios.infrastructure..", "com.ferreteria.usuarios.application.port.out..",
                            "com.ferreteria.usuarios.application",
                            "com.ferreteria.geolocalizacion.infrastructure..",
                            "com.ferreteria.geolocalizacion.application.port.out..",
                            "com.ferreteria.geolocalizacion.application",
                            "com.ferreteria.tiemporeal.infrastructure..", "com.ferreteria.tiemporeal.application.port.out..",
                            "com.ferreteria.tiemporeal.application",
                            "com.ferreteria.sucursales.infrastructure..", "com.ferreteria.sucursales.application")
                    .because("un módulo solo se comunica con otro a través de sus puertos de entrada");

    @ArchTest
    static final ArchRule los_modulos_base_no_dependen_de_ventas_ni_pedidos =
            noClasses().that().resideInAnyPackage(
                            "com.ferreteria.inventario..", "com.ferreteria.usuarios..", "com.ferreteria.sucursales..",
                            "com.ferreteria.geolocalizacion..", "com.ferreteria.eventos..", "com.ferreteria.tiemporeal..",
                            "com.ferreteria.shared..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.ferreteria.pedidos..", "com.ferreteria.ventas..")
                    .because("las dependencias entre módulos van en un solo sentido");

    @ArchTest
    static final ArchRule el_nucleo_compartido_no_depende_de_ningun_modulo =
            noClasses().that().resideInAPackage("com.ferreteria.shared..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.ferreteria.inventario..", "com.ferreteria.usuarios..", "com.ferreteria.sucursales..",
                            "com.ferreteria.geolocalizacion..", "com.ferreteria.eventos..", "com.ferreteria.tiemporeal..",
                            "com.ferreteria.pedidos..", "com.ferreteria.ventas..")
                    .because("lo compartido es la base común: no puede conocer a los módulos");
}
