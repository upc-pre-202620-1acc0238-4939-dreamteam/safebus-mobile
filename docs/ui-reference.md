# Trazabilidad con el informe

Fuente local: `../dreamteam-report/report/03-chapter3.md`. Se inspeccionaron los mock-ups, wireflows y user flows de `docs/ux-ui-mobile-design` para los tres segmentos. El informe y sus imágenes no se modificaron.

| Pantalla / estado | Referencia | Implementación |
|---|---|---|
| Bienvenida | US16 · Bienvenida | Isotipo escudo/bus, marca, selector EN/ES, acceso y registro para pasajeros. |
| Acceso | US16 · Iniciar sesión, Userflow1 | DNI/código institucional, contraseña, visibilidad y error; navegación al rol correspondiente. Las cuentas demo se documentan únicamente en README. |
| Turno asignado | US02 · Turno asignado | Unidad A1B-702, Ruta 23, origen/destino, padrón, horario, ruta y acceso QR. |
| Sin turno | US02 · Sin turno asignado, Userflow1 | Estado inactivo, central de operaciones, actualización y contacto ficticio. |
| Escanear QR | US01 · Escanear QR del conductor | Marco oscuro, guía de credencial, cancelar y resultados simulados válido/inválido. |
| Turno validado | US01 · Turno validado | Confirmación, hora 05:45, credencial, servicio y continuar al turno. |
| Turno activo | US04 · Mi turno, Userflow2 | Estado verde, unidad/horario y emergencia roja dominante en zona inferior. |
| Emergencia enviada | US04 · Emergencia enviada, Userflow2 / Wireflow2 | Critical, Active, hora 22:41 y Ver detalles; activación directa. |
| Pendiente / En atención / Cerrada | Userflow2 / Wireflow2 | Envío pendiente azul, atención Critical y resolución verde con resultado. |
| Mis alertas, detalle, Cuenta | 3.1.2.2–3.1.2.5, seguimiento US04 | Pantallas complementarias según la guía; no tienen mock-up específico de Conductor en 3.1.4. |
| Registro del pasajero | US16 · Cree su cuenta; US23 · Foto del rostro / Contraseña y términos | Tres pasos, DNI de ocho dígitos, DNI existente, foto simulada, contraseña mínima y términos obligatorios. |
| Viaje vacío / bus verificado / activo | US06 | QR, placa/ruta/empresa/conductor, ubicación simulada, inicio y fin manual. Alertas del bus deshabilitadas fuera del viaje. |
| Alertas del bus / sin reportes | US07 | Resúmenes con cantidad, hora y estado; filtros y vista vacía. Sin fotos, mensajes ni identidad de terceros. |
| Solicitud con evidencia / revisión | US08 | Mensaje 1–500 caracteres, foto obligatoria, archivo inválido, previsualización y envío. Evidencia exclusivamente de muestra. |
| Mis solicitudes / detalle | US08, US09 y 3.1.2 | Pendiente de transmisión, progreso 2/3, umbral 3/3, aprobación pendiente, estados finales y emergencia High. La solicitud permanece tras terminar el viaje. |
| Fin de viaje | US24 y user flow del pasajero | Confirmación manual o evento simulado de alejamiento >100 m por 60 s. Alternativa manual sin ubicación. |
| Flota / mapa / detalle | US11 · Flota en lista y mapa | Búsqueda por placa/ruta/conductor, vigencia, aforo, estados Critical/High/pendiente/normal, mapa local con selección y respaldo en lista. |
| Emergencias / atención / historial | US10 · Emergencias | Orden Critical → High → revisiones; Active → In progress → Closed, con resultado obligatorio. |
| Revisión de grupo / decisión | US10 · Revisar grupo | Tres evidencias de muestra autorizadas, aprobar genera High; rechazar exige motivo y conserva la decisión sin emergencia. |
| Capacidad / asignaciones | US12, US13 y 3.1.2 | Entero positivo y asistente conductor → bus → ruta → periodo → confirmación, con conflicto de ejemplo. Complementos de navegación sin mock-up específico. |

## Resolución de diferencias entre láminas y guía

- Algunas imágenes incluyen títulos del informe como `US01` o duplican barras superiores. Esas anotaciones se omiten en el producto.
- La guía fija una paleta y radios concretos que prevalecen sobre colores aproximados y sombras de las imágenes: Primary #0D2C54, Secondary #00796B, Primary Container #DCE7F5; Critical #C62828, High #BF360C, pendiente #F9A825, informativo #1565C0, cerrado #2E7D32, inactivo #6B7480. La solicitud del pasajero es azul, la revisión pendiente ámbar y la emergencia aprobada High naranja. El rojo representa emergencias del conductor.
- Fondos y superficies claros: #F5F7FA / #FFFFFF; oscuros: #0F1720 / #18222E. Texto y bordes mantienen los valores exactos de 3.1.1.
- Se utiliza inglés como idioma inicial y se ofrecen equivalentes en español latinoamericano, aunque las láminas están en español.
- El QR se refiere a la **credencial del conductor**, como indica el flujo US01; la imagen de asignación también menciona el QR físico del bus.
- Se conserva una sola barra global con Turno, Mis alertas y Cuenta. La emergencia permanece disponible en todo el espacio del conductor durante el turno activo, según 3.1.2.5; por eso también aparece en el seguimiento del caso.
- La ruta se representa de forma esquemática y local para cumplir el alcance de UI sin una integración de mapas. No afirma ubicación real.
- El resultado del caso y sus cambios solo se simulan desde Cuenta; el conductor no recibe permisos operativos del supervisor.
- Pasajero: Journey, Bus alerts, My requests y Account. Supervisor: Fleet, Emergencies, Reviews, Assignments y Account, con insignias de casos abiertos. Los tres roles comparten tema y componentes, pero sus datos de demostración son independientes.
- El mapa conserva posiciones ficticias; no consume cartografía. Las fotos se representan por tarjetas de evidencia de muestra. El registro no abre la cámara ni procesa identidad. Las diferencias se señalan en la interfaz y README.

## Tokens y adaptación

Inter usa la escala móvil del informe: 32/40, 24/32, 20/28, 16/24, 14/20 y 12/16 sp; cifras tabulares. Los 12 sp quedan limitados a metadatos. Tarjetas: radio 12 dp y relleno 16 dp; botones/campos: radio 8 dp; botón primario mínimo 56 dp, secundario 48 dp; chips mínimo 32 dp. Se permite crecer a los componentes para respetar fuentes ampliadas. Emergencia mínimo 96 dp, separada 32 dp de contenido y barra inferior. Contenido desplazable con ancho máximo 600 dp, márgenes de 16/24 dp e insets de sistema y teclado.

Los iconos son vectores locales derivados de **Material Symbols Rounded** de Google; Inter se incluye desde Google Fonts. Licencias en `docs/licenses`.

Configuración Compose contrastada con la [documentación oficial del compilador](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler) y [Kotlin integrado en AGP](https://developer.android.com/build/migrate-to-built-in-kotlin).
