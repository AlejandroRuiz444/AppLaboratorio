package com.vitalab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vitalab.app.data.Examen
import com.vitalab.app.data.RECARGO_DOMICILIO
import com.vitalab.app.data.calcularResumen
import com.vitalab.app.data.catalogoExamenes
import com.vitalab.app.data.diaCorto
import com.vitalab.app.data.formatoFecha
import com.vitalab.app.data.formatoPesos
import com.vitalab.app.data.local.Cita
import com.vitalab.app.data.local.CitaCompleta
import com.vitalab.app.data.local.CitaExamen
import com.vitalab.app.data.local.EstadoCita
import com.vitalab.app.data.local.LineaCita
import com.vitalab.app.data.local.Paciente
import com.vitalab.app.data.mesCorto
import java.time.LocalDate

// Piezas compartidas por las pantallas de citas (consulta, detalle, agendar y reprogramar).

/** Estado de una cita con color, ícono y texto: el color nunca es la única señal. */
@Composable
fun EtiquetaEstadoCita(estado: EstadoCita, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val (fondo, texto, icono) = when (estado) {
        EstadoCita.PROGRAMADA -> Triple(c.secondaryContainer, c.onSecondaryContainer, Icons.Filled.DateRange)
        EstadoCita.COMPLETADA -> Triple(c.primaryContainer, c.onPrimaryContainer, Icons.Filled.CheckCircle)
        EstadoCita.CANCELADA -> Triple(c.tertiaryContainer, c.onTertiaryContainer, Icons.Filled.Close)
    }
    Surface(modifier, color = fondo, contentColor = texto, shape = MaterialTheme.shapes.small) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(estado.etiqueta, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Dato de una línea con un ícono decorativo a la izquierda (fecha, modalidad...). */
@Composable
fun DatoConIcono(icono: ImageVector, texto: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Dato "etiqueta: valor" de una cita. A diferencia de `FilaResumen` (mitad y mitad), la etiqueta ocupa lo justo y el valor
 * el resto: así un correo o una dirección larga no se parte en dos líneas sin necesidad.
 */
@Composable
fun FilaDato(etiqueta: String, valor: String, modifier: Modifier = Modifier, destacado: Boolean = false) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = if (destacado) Alignment.CenterVertically else Alignment.Top
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(16.dp))
        Text(
            valor,
            style = if (destacado) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Medium,
            color = if (destacado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Examen de una cita con el precio que se cobró al agendar (no el actual del catálogo). */
@Composable
fun FilaExamenCita(linea: LineaCita, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(linea.examen.nombre, style = MaterialTheme.typography.titleSmall)
            Text("Muestra: ${linea.examen.muestra}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.size(12.dp))
        Text(formatoPesos(linea.precio), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

/** Mensaje de error bajo un grupo de opciones. No dibuja nada si `texto` es null. */
@Composable
fun TextoError(texto: String?, modifier: Modifier = Modifier) {
    if (texto != null) {
        Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = modifier.padding(start = 4.dp))
    }
}

/**
 * Opción de una elección única (día, hora, modalidad): rellena con el color primario cuando está elegida y
 * blanca con sombra suave si no. Objetivo táctil de al menos 48 dp. `descripcion` reemplaza lo que lee TalkBack.
 */
@Composable
fun OpcionSeleccionable(
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    descripcion: String? = null,
    contenido: @Composable () -> Unit
) {
    val c = MaterialTheme.colorScheme
    val forma = MaterialTheme.shapes.medium
    val sinDescripcion = Modifier
    Box(
        modifier
            .heightIn(min = 48.dp)
            .shadow(if (seleccionada) 0.dp else 2.dp, forma)
            .clip(forma)
            .background(if (seleccionada) c.primary else c.surfaceContainerHighest)
            .selectable(selected = seleccionada, onClick = onClick, role = Role.RadioButton)
            .then(if (descripcion != null) Modifier.clearAndSetSemantics { contentDescription = descripcion } else sinDescripcion)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides if (seleccionada) c.onPrimary else c.onSurface) { contenido() }
    }
}

/** Encabezado numerado de un paso del formulario; el círculo se llena con una marca cuando el paso está completo. */
@Composable
fun PasoFormulario(
    numero: Int,
    titulo: String,
    completo: Boolean,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.clearAndSetSemantics {
                heading()
                contentDescription = "Paso $numero: $titulo" + if (completo) ", completo" else ""
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(28.dp).background(if (completo) c.primary else c.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (completo) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(16.dp))
                } else {
                    Text("$numero", style = MaterialTheme.typography.labelLarge, color = c.onPrimaryContainer)
                }
            }
            Text(titulo, style = MaterialTheme.typography.titleMedium)
        }
        contenido()
    }
}

private const val COLUMNAS = 3

/** Rejilla de opciones de igual ancho, `COLUMNAS` por fila (se ven todas sin desplazar). */
@Composable
private fun <T> RejillaOpciones(items: List<T>, opcion: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(COLUMNAS).forEach { fila ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { opcion(it, Modifier.weight(1f)) }
                repeat(COLUMNAS - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Próximos días hábiles (`epochDay`) para elegir. */
@Composable
fun SelectorFecha(
    fechas: List<Long>,
    seleccionada: Long?,
    onSeleccionar: (Long) -> Unit,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RejillaOpciones(fechas) { dia, mod ->
            val fecha = LocalDate.ofEpochDay(dia)
            OpcionSeleccionable(dia == seleccionada, { onSeleccionar(dia) }, mod, descripcion = formatoFecha(fecha)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(diaCorto(fecha), style = MaterialTheme.typography.labelMedium)
                    Text("${fecha.dayOfMonth}", style = MaterialTheme.typography.titleLarge)
                    Text(mesCorto(fecha), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        TextoError(error)
    }
}

/** Franjas horarias; si la orden exige ayuno avisa por qué solo hay franjas tempranas. */
@Composable
fun SelectorHora(
    franjas: List<String>,
    seleccionada: String?,
    onSeleccionar: (String) -> Unit,
    ayunoHoras: Int,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (ayunoHoras > 0) {
            Text(
                "Tu orden requiere ayuno de $ayunoHoras horas: solo mostramos franjas hasta las 9:00 a.m.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        RejillaOpciones(franjas) { franja, mod ->
            OpcionSeleccionable(franja == seleccionada, { onSeleccionar(franja) }, mod) {
                Text(
                    franja,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (franja == seleccionada) FontWeight.Bold else null,
                    textAlign = TextAlign.Center
                )
            }
        }
        TextoError(error)
    }
}

/** En sede o a domicilio; la dirección (validada por el ViewModel) aparece solo a domicilio. */
@Composable
fun SelectorModalidad(
    domicilio: Boolean,
    direccion: String,
    errorDireccion: String?,
    onDomicilioChange: (Boolean) -> Unit,
    onDireccionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpcionModalidad(Icons.Filled.LocationOn, "En sede", "Sin recargo", !domicilio, { onDomicilioChange(false) }, Modifier.weight(1f))
            OpcionModalidad(
                Icons.Filled.Home, "A domicilio", "+" + formatoPesos(RECARGO_DOMICILIO), domicilio,
                { onDomicilioChange(true) }, Modifier.weight(1f)
            )
        }
        if (domicilio) {
            CampoValidado(direccion, onDireccionChange, "Dirección de la toma", errorDireccion, mostrarError = true, icono = Icons.Filled.Place)
        }
    }
}

@Composable
private fun OpcionModalidad(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    OpcionSeleccionable(seleccionada, onClick, modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            Text(detalle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Cita de ejemplo para `@Preview` y pruebas (no se usa en la app). */
internal fun citaDeEjemplo(
    id: Long = 1,
    estado: EstadoCita = EstadoCita.PROGRAMADA,
    domicilio: Boolean = false,
    paciente: String = "Marta Ruiz Gómez",
    codigo: String = "VL-4821",
    examenes: List<Examen> = catalogoExamenes.take(3),
    fecha: LocalDate = LocalDate.of(2026, 11, 17)
): CitaCompleta {
    val resumen = calcularResumen(examenes, adultoMayor = false, domicilio = domicilio)
    val cita = Cita(
        id = id, codigo = codigo, pacienteId = 1, fecha = fecha.toEpochDay(), hora = "7:00 a.m.",
        domicilio = domicilio, direccion = if (domicilio) "Cra 15 # 82-30, apto 402" else "", estado = estado, adultoMayor = false,
        subtotal = resumen.subtotal, descuentoPaquete = resumen.descuentoPaquete,
        descuentoAdultoMayor = resumen.descuentoAdultoMayor, recargoDomicilio = resumen.recargoDomicilio
    )
    val pac = Paciente(id = 1, nombre = paciente, documento = "1020304050", telefono = "3101234567", correo = "marta.ruiz@correo.com")
    return CitaCompleta(cita, pac, examenes, examenes.map { CitaExamen(id, it.id, it.precio) })
}
