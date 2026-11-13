package com.vitalab.app.viewmodel

import kotlinx.coroutines.flow.SharingStarted

/** Mantiene vivo el flujo 5 s sin colectores: sobrevive a una rotación sin volver a consultar Room. */
internal val Suscripcion = SharingStarted.WhileSubscribed(5_000)

/**
 * Mensaje de error de un campo, o null. Se muestra solo si el usuario ya escribió algo o ya intentó
 * enviar: así el formulario no aparece en rojo antes de tocarlo.
 */
internal fun errorVisible(valor: String, intentado: Boolean, validar: (String) -> String?): String? =
    if (intentado || valor.isNotEmpty()) validar(valor) else null
