package com.vitalab.app.data

enum class Categoria(val etiqueta: String) {
    HEMATOLOGIA("Hematología"),
    QUIMICA("Química sanguínea"),
    HORMONAS("Hormonas"),
    ORINA("Uroanálisis"),
    INFECCIOSAS("Infecciosas")
}

/** Información principal del negocio: un examen que ofrece el laboratorio. */
data class Examen(
    val id: Int,
    val nombre: String,
    val categoria: Categoria,
    val precio: Int,
    val descripcion: String,
    val muestra: String,
    val ayunoHoras: Int,
    val horasEntrega: Int,
    val indicacion: String = ""
)

val catalogoExamenes = listOf(
    Examen(1, "Cuadro hemático", Categoria.HEMATOLOGIA, 25_000,
        "Evalúa glóbulos rojos, glóbulos blancos y plaquetas.", "Sangre", 0, 4),
    Examen(2, "Glucosa en ayunas", Categoria.QUIMICA, 12_000,
        "Mide el nivel de azúcar en la sangre.", "Sangre", 8, 4),
    Examen(3, "Perfil lipídico", Categoria.QUIMICA, 48_000,
        "Colesterol total, HDL, LDL y triglicéridos.", "Sangre", 12, 6,
        "Evita comidas grasas y alcohol 24 horas antes."),
    Examen(4, "Hemoglobina glicosilada (HbA1c)", Categoria.QUIMICA, 45_000,
        "Promedio de glucosa de los últimos 3 meses.", "Sangre", 0, 24),
    Examen(5, "Creatinina", Categoria.QUIMICA, 15_000,
        "Evalúa el funcionamiento de los riñones.", "Sangre", 0, 6),
    Examen(6, "TSH", Categoria.HORMONAS, 38_000,
        "Hormona estimulante de la tiroides.", "Sangre", 0, 24),
    Examen(7, "Prueba de embarazo en sangre", Categoria.HORMONAS, 30_000,
        "Detecta la hormona β-hCG.", "Sangre", 0, 4),
    Examen(8, "Parcial de orina", Categoria.ORINA, 14_000,
        "Análisis físico, químico y microscópico de la orina.", "Orina", 0, 4,
        "Trae la primera orina de la mañana en frasco estéril."),
    Examen(9, "Urocultivo", Categoria.ORINA, 40_000,
        "Identifica bacterias que causan infección urinaria.", "Orina", 0, 72,
        "No tomes antibióticos los 3 días anteriores."),
    Examen(10, "Antígeno Hepatitis B", Categoria.INFECCIOSAS, 42_000,
        "Detecta infección por el virus de la Hepatitis B.", "Sangre", 0, 24),
    Examen(11, "VIH (4ª generación)", Categoria.INFECCIOSAS, 50_000,
        "Detecta anticuerpos y antígeno p24 del VIH.", "Sangre", 0, 24,
        "Debes firmar el consentimiento informado en la sede.")
)
