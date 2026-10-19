package com.vitalab.app.data

import java.time.DayOfWeek
import java.time.LocalDate

const val MINIMO_PAQUETE = 3
const val RECARGO_DOMICILIO = 20_000

data class Resumen(
    val subtotal: Int,
    val descuentoPaquete: Int,
    val descuentoAdultoMayor: Int,
    val recargoDomicilio: Int
) {
    val total: Int get() = subtotal - descuentoPaquete - descuentoAdultoMayor + recargoDomicilio
}

data class Cita(
    val codigo: String,
    val paciente: String,
    val documento: String,
    val telefono: String,
    val correo: String,
    val fecha: LocalDate,
    val hora: String,
    val domicilio: Boolean,
    val direccion: String,
    val examenes: List<Examen>,
    val resumen: Resumen
)

/** Paquete de 3+ exámenes: 10%. Adulto mayor: 15%. Toma a domicilio: recargo fijo. */
fun calcularResumen(examenes: List<Examen>, adultoMayor: Boolean, domicilio: Boolean): Resumen {
    val subtotal = examenes.sumOf { it.precio }
    return Resumen(
        subtotal = subtotal,
        descuentoPaquete = if (examenes.size >= MINIMO_PAQUETE) subtotal * 10 / 100 else 0,
        descuentoAdultoMayor = if (adultoMayor) subtotal * 15 / 100 else 0,
        recargoDomicilio = if (domicilio && examenes.isNotEmpty()) RECARGO_DOMICILIO else 0
    )
}

/** Une la preparación de todos los exámenes: el ayuno más largo, las muestras y la entrega más tardía. */
fun indicacionesPreparacion(examenes: List<Examen>): List<String> {
    if (examenes.isEmpty()) return emptyList()
    val ayuno = examenes.maxOf { it.ayunoHoras }
    val muestras = examenes.map { it.muestra }.distinct().joinToString(" y ")
    return buildList {
        add(if (ayuno > 0) "Ayuno de $ayuno horas (solo puedes tomar agua)." else "No requiere ayuno.")
        add("Muestras a tomar: $muestras.")
        addAll(examenes.map { it.indicacion }.filter { it.isNotBlank() }.distinct())
        add("Tus resultados estarán listos en ${textoEntrega(examenes.maxOf { it.horasEntrega })}.")
    }
}

val FRANJAS = listOf(
    "6:00 a.m.", "6:30 a.m.", "7:00 a.m.", "7:30 a.m.", "8:00 a.m.",
    "9:00 a.m.", "10:00 a.m.", "11:00 a.m.", "2:00 p.m.", "3:00 p.m."
)

/** Si la orden exige ayuno, solo se ofrecen franjas hasta las 9:00 a.m. */
fun franjasDisponibles(ayunoHoras: Int): List<String> =
    if (ayunoHoras > 0) FRANJAS.take(6) else FRANJAS

/** Próximos días hábiles (el laboratorio no atiende domingos). */
fun proximasFechas(desde: LocalDate, cantidad: Int = 6): List<LocalDate> =
    generateSequence(desde.plusDays(1)) { it.plusDays(1) }
        .filter { it.dayOfWeek != DayOfWeek.SUNDAY }
        .take(cantidad)
        .toList()

fun generarCodigoCita(): String = "VL-" + (1000..9999).random()

// Validaciones del formulario: devuelven el mensaje de error o null si el valor es válido.
fun validarNombre(v: String) = if (v.trim().length < 3) "Escribe tu nombre completo" else null
fun validarDocumento(v: String) = if (!v.matches(Regex("\\d{6,10}"))) "Solo números, entre 6 y 10 dígitos" else null
fun validarTelefono(v: String) = if (!v.matches(Regex("3\\d{9}"))) "Celular de 10 dígitos que empiece por 3" else null
fun validarCorreo(v: String) =
    if (!v.trim().matches(Regex("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+"))) "Correo electrónico no válido" else null
fun validarDireccion(v: String) = if (v.trim().length < 6) "Escribe la dirección completa" else null

// Formatos de texto
fun formatoPesos(valor: Int): String =
    "$" + valor.toString().reversed().chunked(3).joinToString(".").reversed()

fun textoEntrega(horas: Int): String = when {
    horas < 24 -> "$horas horas"
    horas / 24 == 1 -> "1 día"
    else -> "${horas / 24} días"
}

private val DIAS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
private val MESES = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)

fun formatoFecha(f: LocalDate) = "${DIAS[f.dayOfWeek.value - 1]} ${f.dayOfMonth} de ${MESES[f.monthValue - 1]}"
fun diaCorto(f: LocalDate) = DIAS[f.dayOfWeek.value - 1].take(3)
fun mesCorto(f: LocalDate) = MESES[f.monthValue - 1].take(3)
