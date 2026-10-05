# SafeBus · Android

Prototipo visual nativo para **Conductor, Pasajero y Supervisor**, desarrollado en **Kotlin, Jetpack Compose y Material 3**. Basado en los mock-ups, wireflows y user flows de la sección **3.1.4**, la paleta y tipografía de **3.1.1**, y la navegación por rol de **3.1.2** de `dreamteam-report/report/03-chapter3.md`.

## Cuentas demo

Las credenciales se documentan aquí y **no se muestran en la pantalla de inicio de sesión**. Escriba el identificador en el campo «DNI o código institucional»; la cuenta abre directamente el espacio de su rol.

| Segmento | DNI o código institucional | Contraseña |
|---|---|---|
| Conductor | `conductor.demo` | `SafeBus123` |
| Pasajero | `76543210` | `SafeBus123` |
| Supervisor | `supervisor.demo` | `SafeBus123` |

Son cuentas ficticias, resueltas localmente. Una combinación incorrecta muestra un error. Para cambiar de rol, vaya a **Cuenta → Cerrar sesión**.

## Ejecutar

Abra el repositorio en Android Studio y ejecute `app` en un emulador o dispositivo con Android 8+ (API 26). Configure el SDK en `local.properties` o mediante Android Studio.

Requisitos: JDK 17+ compatible con Gradle 9.6 (validado con JBR 25), SDK Platform 37 y Build Tools 36.0.0. **Target SDK 35 (Android 15)**. El wrapper, AGP 9.4.0 y las dependencias tienen versiones fijadas; AGP incorpora Kotlin.

```powershell
$env:JAVA_HOME = 'D:/AndroidStudio/jbr' # adapte la ruta
./gradlew.bat assembleDebug lintDebug
./gradlew.bat connectedDebugAndroidTest # requiere emulador o dispositivo
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Recorridos

### Conductor

1. Inicie sesión con su cuenta demo.
2. Turno asignado → Escanear QR → Simular QR válido → Turno validado → Ir a mi turno.
3. **Emergencia** activa el caso con un toque, sin formulario ni confirmación previa.
4. Consulte Mis alertas y el detalle; cierre el turno con confirmación.

En **Cuenta → Escenarios del prototipo** puede simular falta de asignación, historial vacío, desconexión y estados En atención/Cerrada. La emergencia permanece disponible durante el turno activo en los tres destinos del conductor.

### Pasajero

1. Acceda con la cuenta demo o recorra **Crear cuenta → DNI → Foto de muestra → Contraseña y términos**. El registro acepta un DNI ficticio de ocho dígitos, contraseña de al menos ocho caracteres y aceptación de términos. El DNI demo ya existe. La cuenta creada solo se conserva durante esta ejecución del prototipo.
2. **Mi viaje → Escanear QR del bus → Bus verificado → Iniciar viaje**. La ubicación se simula; también puede continuar y finalizar manualmente sin activarla.
3. **Alertas del bus** se habilita durante el viaje y muestra resúmenes públicos, sin identidades ni evidencia de otros pasajeros.
4. **Enviar solicitud de pánico** exige mensaje de 1–500 caracteres y foto de muestra. Revise y confirme el envío. Puede simular un archivo inválido.
5. **Mis solicitudes** muestra progreso de 2/3 pasajeros, espera de aprobación, evidencia propia y seguimiento. Terminar el viaje no elimina la solicitud.
6. En **Cuenta**, simule desconexión/recuperación, reportes vacíos, permisos de ubicación, alejamiento del bus y los estados de solicitud: Pendiente, Recopilando, Esperando aprobación, Expirada, No aprobada, Tardía, Activa, En atención y Cerrada.

Una solicitud no es todavía una emergencia. El umbral ilustrado es **tres pasajeros distintos en cinco minutos**; la aprobación de la empresa genera una emergencia **High**. La solicitud pendiente de transmisión no cuenta para ese umbral. El alejamiento >100 m durante 60 segundos se representa mediante un evento de demostración, sin GPS ni temporizador real.

### Supervisor

1. **Flota** permite buscar por placa, ruta o conductor y filtrar ubicación/aforo. Alterne lista y mapa esquemático, seleccione un marcador y abra el detalle. **Editar capacidad** acepta un entero positivo.
2. **Emergencias** ordena los casos Critical del conductor, High de pasajeros y revisiones pendientes. Abra un caso, inicie atención y registre un resultado obligatorio para cerrarlo. Los casos cerrados pasan a Historial.
3. **Revisiones → Revisar grupo** presenta tres mensajes y evidencias de muestra. **Aprobar** crea el caso High. **No aprobar** requiere motivo y no crea una emergencia.
4. **Asignaciones → Nueva asignación** guía por conductor, bus, ruta, periodo y confirmación. El turno de mañana de Carlos Mamani/A1B-702 representa un conflicto; elegir la tarde permite continuar.
5. En **Cuenta**, simule la indisponibilidad del mapa para revisar la lista de respaldo o restablezca los casos iniciales.

## Alcance y estilo

- UI pura con navegación y estado local temporal. Sin backend, API, base de datos remota, autenticación real ni lógica compleja de negocio.
- QR, fotos, evidencias, mapa, aforo, ubicación, horas y envío de alertas son simulados. No se solicitan permisos de cámara, ubicación, llamadas ni acceso a Internet.
- La demostración del pasajero conserva una solicitud de ejemplo por sesión; un nuevo envío reemplaza ese ejemplo. Asignaciones muestra la última creación de ejemplo. Los roles usan fixtures independientes, sin sincronización entre cuentas. Cerrar sesión restablece los casos.
- `rememberSaveable` conserva el estado visual ante recreación de la actividad; no sustituye persistencia de negocio.
- Paleta oficial: Primary **#0D2C54**, Secondary **#00796B**, Critical **#C62828**, High **#BF360C**, aprobación pendiente **#F9A825**, informativo **#1565C0**, cerrado **#2E7D32** e inactivo **#6B7480**.
- Inter y Material Symbols Rounded incluidos localmente. Temas claro/oscuro del sistema, texto ampliable e interfaces completas en inglés (inicial) y español.

Consulte [la trazabilidad visual](docs/ui-reference.md) y [la validación](docs/validation.md). Hay vistas previas Compose en `ui/Previews.kt` y pruebas instrumentadas de los tres segmentos en `app/src/androidTest`.
