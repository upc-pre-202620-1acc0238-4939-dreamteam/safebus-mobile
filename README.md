# SafeBus · Android

Aplicación nativa conectada a **SafeBus API** para **Conductor, Pasajero y Supervisor**, desarrollada en Kotlin y Jetpack Compose. El punto de entrada es `com.dreamteam.safebus.MainActivity`: autenticación, flota, turnos, viajes, emergencias y aforo utilizan el backend; ya no se ejecutan con las cuentas locales del prototipo.

Esta guía corresponde al código de `develop` en **`bd8ea39`**. El APK de prueba generado desde esa revisión se llama **`safebus-develop-bd8ea39-debug.apk`**. La versión interna continúa siendo `0.1.0` (`versionCode = 1`).

## Requisitos

| Componente | Configuración del proyecto |
|---|---|
| Java | **JDK 21**; validado con 21.0.12 |
| Android Studio | Una versión compatible con **Android Gradle Plugin 9.5.1** |
| Android SDK | Platform **37**, Build Tools **36.0.0**, Platform Tools y Emulator |
| Gradle | **9.6.0**, mediante el wrapper incluido |
| UI | Kotlin **2.4.21**, Compose BOM **2026.09.00** |
| Dispositivo | Android **8.0 / API 26** o posterior; target SDK **37** |
| Backend | Repositorio `safebus-api`, JDK 21 y su Maven Wrapper |

En Android Studio, configure **Gradle JDK = JDK 21** y descargue los componentes del SDK desde SDK Manager. `local.properties` debe apuntar a su SDK; Android Studio puede generarlo. No suba rutas locales ni credenciales privadas al repositorio.

Los comandos siguientes usan PowerShell en Windows. En Linux/macOS utilice `./gradlew` y `./mvnw` en lugar de `./gradlew.bat` y `./mvnw.cmd`, y adapte las variables de entorno y rutas.

## 1. Iniciar el backend local

Desde la carpeta **`safebus-api`**, abra una terminal y ejecute:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21.0.12' # adapte la ruta
./mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev' '-Dspring-boot.run.arguments=--server.address=127.0.0.1 --server.port=8080'
```

Mantenga esta terminal abierta mientras prueba la aplicación. Si ya hay un backend escuchando en el puerto 8080, compruebe su estado antes de iniciar otro proceso.

El perfil `dev` crea los datos iniciales al encontrar una base vacía. Por defecto utiliza H2 persistente en `./data/safebus`, relativa a la carpeta desde la que se inicia el backend. Los datos sobreviven a los reinicios. Las variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`, si están definidas, pueden cambiar esta configuración.

Compruebe la disponibilidad desde otra terminal:

```powershell
Invoke-RestMethod 'http://127.0.0.1:8080/api/v1/health'
```

Debe devolver `status: UP`. La documentación de la API está en [Swagger UI](http://127.0.0.1:8080/swagger-ui.html) y [OpenAPI](http://127.0.0.1:8080/v3/api-docs).

### Demostración o pruebas con datos temporales

Para disponer de una base nueva y aislada en cada ejecución, use este comando **en lugar del anterior**:

```powershell
./mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev' '-Dspring-boot.run.arguments=--server.address=127.0.0.1 --server.port=8080 --spring.datasource.url=jdbc:h2:mem:safebus_demo;MODE=MySQL;DB_CLOSE_DELAY=-1'
```

Esta alternativa no borra la base persistente, pero **pierde sus propios datos al detener el backend**. Después de reiniciarlo, vuelva a iniciar sesión y registre de nuevo los pasajeros de prueba. Termine los viajes y turnos antes de detenerlo para evitar referencias locales a datos que ya no existen.

## 2. Cuentas de desarrollo

Escriba el identificador en «DNI o código institucional». Las credenciales se documentan únicamente aquí; no aparecen como sugerencias en el inicio de sesión.

| Rol | Identificador | Contraseña | Disponibilidad |
|---|---|---|---|
| Supervisor | `sup-001` | `Safebus2024!` | Creado por el backend `dev` en una base vacía |
| Conductor | `drv-001` | `Safebus2024!` | Creado por el backend `dev` en una base vacía |
| Pasajero | `76543210` | `SafeBus123` | Ejemplo para **registrar primero desde la aplicación** |

**El backend no crea un pasajero demo.** En «Crear cuenta», registre el DNI ficticio indicado, una foto JPEG/PNG desde el selector de Android, una contraseña de al menos ocho caracteres y la aceptación de términos. Si el DNI ya existe, utilice su contraseña real o registre otro DNI ficticio de ocho dígitos. No se trata de un acceso local ni de una foto simulada.

El inicializador solo inserta los datos demo cuando no hay empresas. Una base ya poblada no restablece contraseñas ni vuelve a crear las cuentas automáticamente. Las antiguas cuentas `conductor.demo` y `supervisor.demo` del prototipo no son credenciales del backend.

## 3. Ejecutar desde Android Studio

1. Con el árbol de trabajo limpio, actualice el código: `git switch develop` y `git pull --ff-only`.
2. Abra **la carpeta raíz de `safebus-mobile`** en Android Studio y sincronice Gradle.
3. Configure JDK 21, instale SDK 37 y seleccione la variante **debug** del módulo `app`.
4. Inicie el backend del paso 1 y un emulador Android desde Device Manager.
5. Ejecute la configuración `app`. En API 37, conceda el permiso de acceso a la red local cuando la aplicación lo solicite.
6. En la configuración de conexión de la aplicación, utilice **`http://10.0.2.2:8080`** y guarde. Es el valor predeterminado de debug y permite llegar al equipo anfitrión desde el emulador estándar de Android.
7. Abra «Contactar a SafeBus» → «Comprobar servicio». Debe aparecer «El servicio está disponible». Regrese e inicie sesión.

La URL debe ser la **base del servidor, sin `/api/v1`**. La aplicación añade las rutas de cada operación. Una URL guardada desde la interfaz tiene prioridad sobre la incluida al compilar, incluso después de reinstalar con `adb install -r`.

## 4. Generar e instalar el APK

Desde la carpeta `safebus-mobile`:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21.0.12' # adapte la ruta
./gradlew.bat assembleDebug --console=plain '-PsafebusApiUrl=http://10.0.2.2:8080'
```

El resultado instalable y firmado con la clave de depuración se encuentra en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Para esta entrega se conserva además una copia identificable en la misma carpeta:

```text
app/build/outputs/apk/debug/safebus-develop-bd8ea39-debug.apk
```

Los APK son archivos generados y no se incluyen en Git. El comando produce `app-debug.apk`; el nombre de la copia identifica la revisión de esta entrega. Para instalarlo, agregue `platform-tools` del SDK al PATH y use el identificador que muestre `adb devices`:

```powershell
adb devices
$deviceSerial = 'emulator-5554' # sustituya por el identificador real
adb -s $deviceSerial install -r app/build/outputs/apk/debug/app-debug.apk
adb -s $deviceSerial shell am start -n com.dreamteam.safebus/.MainActivity
```

Si utiliza la copia entregada, sustituya la ruta de `app-debug.apk` por la ruta de `safebus-develop-bd8ea39-debug.apk`. También puede arrastrar el APK a la ventana del emulador. Para editar y depurar con Android Studio, abra el proyecto fuente y ejecute `app`.

### Teléfono físico conectado por USB

1. Active las opciones de desarrollador y la depuración USB; acepte la autorización del equipo en el teléfono.
2. Mantenga el backend en `127.0.0.1:8080` y configure el túnel:

```powershell
adb devices
$deviceSerial = 'IDENTIFICADOR_DEL_TELEFONO'
adb -s $deviceSerial reverse tcp:8080 tcp:8080
adb -s $deviceSerial install -r app/build/outputs/apk/debug/app-debug.apk
```

3. En la aplicación, cambie la URL a **`http://127.0.0.1:8080`**, guarde y compruebe el servicio. En Android que lo requiera, conceda acceso a la red local.

Repita `adb reverse` si el túnel se pierde al desconectar o reiniciar. `10.0.2.2` es una dirección especial del emulador, no la dirección del backend para un teléfono físico.

### Backend remoto

Use la URL **HTTPS real** de su despliegue, con un certificado válido, desde la configuración de conexión o al compilar:

```powershell
./gradlew.bat assembleDebug '-PsafebusApiUrl=https://SU_HOST_REAL'
```

`SU_HOST_REAL` es un marcador que debe sustituir. El APK entregado apunta al backend local; no incorpora un servicio alojado ni inicia el backend por sí mismo.

Debug solo permite HTTP para `10.0.2.2`, `127.0.0.1` y `localhost`. Una URL HTTP de la red Wi-Fi, como `192.168.x.x`, será rechazada; utilice el túnel USB o HTTPS. Release exige HTTPS y no tiene una URL predeterminada. `assembleRelease` produce un APK **sin firma de distribución** mientras no se configure una firma; para esta prueba instale el APK debug entregado.

## 5. Recorridos conectados por rol

### Conductor

1. Inicie sesión con `drv-001`. En una base dev recién creada existe una asignación para el bus `ABC123` y la ruta `MainRoute`.
2. Abra el turno asignado e introduzca o escanee la credencial **`DEV-DRIVER-QR-001`** para activarlo.
3. Conceda ubicación para transmitir posiciones reales. Pruebe la emergencia y consulte su seguimiento; el supervisor podrá atenderla desde su propia sesión.
4. Cierre el turno con la confirmación correspondiente.

La asignación inicial se crea desde una hora antes hasta ocho horas después del primer arranque. **No se renueva al reiniciar una base persistente.** Si expiró o el turno ya fue cerrado, el supervisor debe crear una asignación vigente y sin solapamientos. Puede usar una base temporal nueva para repetir la demostración desde cero.

### Pasajero

1. Registre su cuenta antes del primer inicio de sesión. Prepare una imagen JPEG/PNG en el dispositivo o emulador.
2. Mantenga un turno activo de conductor para el bus. En una base recién creada, el QR completo del bus demo es **`safebus://bus/1?code=DEV-BUS-QR-001`**.
3. Inicie el viaje y consulte la verificación del bus, ubicación e historial público. El identificador `1` corresponde a la base recién creada; para otros buses utilice el QR completo proporcionado al crearlos.
4. Envíe una solicitud con mensaje, imagen real y confirmación. Consulte su seguimiento y termine el viaje cuando corresponda.

Una solicitud de pasajero pasa por las reglas y revisión del backend antes de convertirse en emergencia. Los resúmenes públicos no muestran evidencias privadas de otros pasajeros. Para probar varias cuentas a la vez, use distintos dispositivos o emuladores.

El escaneo abre una aplicación compatible con el intent de ZXing. Si no está instalada, utilice la entrada manual del QR. Conserve la URI completa del bus, con su identificador y código, para disponer de las consultas de historial y ubicación.

### Supervisor

1. Inicie sesión con `sup-001` y consulte la flota, la búsqueda por placa, la ubicación y el aforo. El enlace al mapa requiere coordenadas y una aplicación capaz de abrirlas.
2. Registre buses, conductores y rutas; cree asignaciones con sus identificadores y fechas válidas. Guarde las credenciales mostradas al crear los recursos.
3. Revise solicitudes de pasajeros, apruebe o rechace según corresponda y atienda/cierre emergencias con la información solicitada.
4. En las fuentes de conteo del bus, registre, rote o revoque claves. La clave nueva se muestra al emitirla: consérvela para configurar esa fuente.
5. Para probar el contador manual, tenga un turno activo y una clave vigente; confirme que el bus está vacío, inicialice el aforo en cero y registre entradas, salidas y señales de actividad. Reintente los envíos pendientes antes de continuar.

El contador manual transmite eventos reales al backend. No debe utilizarse simultáneamente con un sensor físico como si ambos fueran la misma fuente.

## 6. Verificación y pruebas

Desde el repositorio móvil:

```powershell
./gradlew.bat test --console=plain
./gradlew.bat lintDebug lintRelease --console=plain
./gradlew.bat assembleDebug assembleDebugAndroidTest --console=plain
```

Las pruebas Android que ejercitan operaciones reales necesitan un backend **dev de pruebas** disponible en `http://10.0.2.2:8080`, con el supervisor inicial. Utilice la base temporal descrita arriba: estas pruebas crean datos. No las ejecute contra una base de producción.

Use un emulador dedicado, sin una sesión previa abierta y con las animaciones del sistema desactivadas desde Opciones de desarrollador. Una prueba interrumpida puede dejar una sesión abierta y afectar los recorridos siguientes; repita la suite con un entorno de pruebas limpio si ocurre.

Para ejecutar la suite en un emulador API 33, con un único dispositivo conectado:

```powershell
./gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.backend=true' --console=plain
```

En API 37, puede instalar primero ambos APK y conceder acceso local antes de iniciar las pruebas:

```powershell
$deviceSerial = 'emulator-5554' # adapte el identificador
adb -s $deviceSerial install -r app/build/outputs/apk/debug/app-debug.apk
adb -s $deviceSerial install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s $deviceSerial shell pm grant com.dreamteam.safebus android.permission.ACCESS_LOCAL_NETWORK
adb -s $deviceSerial shell am instrument -w -e backend true com.dreamteam.safebus.test/androidx.test.runner.AndroidJUnitRunner
```

La suite de esta revisión tiene **78 pruebas JVM** ejecutadas por `testDebugUnitTest` y **62 pruebas Android**. Sin `backend=true`, las pruebas condicionadas a integración se omiten. Para validar la conexión real, revise que se hayan ejecutado, sin fallos ni omisiones, y no solo el mensaje de compilación. La suite Android incluye pruebas del prototipo conservadas y pruebas de la aplicación conectada.

Los informes de Gradle quedan en `app/build/reports/tests`, `app/build/reports/androidTests/connected` y `app/build/reports/lint-results-*.html`. La ejecución directa con `adb shell am instrument` imprime su resultado en la terminal.

## Solución de problemas

| Síntoma | Qué comprobar |
|---|---|
| No conecta al servicio | Backend activo, `/api/v1/health`, puerto y URL: `10.0.2.2` en emulador o `127.0.0.1` con túnel USB en teléfono. |
| Permiso de red local | Conceder el permiso solicitado en Android 17/API 37; revisar los permisos de la aplicación si fue denegado. |
| URL rechazada | Sin `/api/v1`, parámetros ni credenciales en la URL; HTTP solo en los tres hosts locales admitidos por debug. |
| Inicio de sesión rechazado | Usar las cuentas reales de la tabla, respetando mayúsculas; el pasajero debe registrarse. |
| No aparece una asignación utilizable | Revisar periodo, estado y conflictos; la asignación dev inicial caduca y no se regenera en una base existente. |
| El pasajero no puede verificar el bus | Activar primero el turno del conductor y usar el QR completo del bus correcto. |
| No abre escáner o mapa | Instalar una aplicación compatible o introducir el QR manualmente; el mapa requiere coordenadas. |
| Se conserva una URL anterior | Cambiarla y guardarla desde la configuración; reinstalar con `-r` conserva las preferencias. |
| APK incompatible por firma | Se necesita la misma firma para actualizar una instalación. Desinstalar borra los datos locales, incluidos envíos pendientes; resuélvalos antes. |

## Arquitectura y alcance actual

El paquete conectado es `com.dreamteam.safebus`, organizado en los contextos `iam`, `fleet`, `trip`, `passenger`, `safetycase` y `shared`, con capas `domain`, `data` y `ui`. El backend decide los estados de negocio; la aplicación conserva datos locales cifrados y envíos pendientes para los flujos que admiten reintento.

Se mantiene el estilo oscuro, acento principal **#C3F400**, secundario **#596D0B**, rojo de emergencia e Inter. Las pantallas heredadas bajo `com.safebus.driver` y los documentos de `docs/` describen el prototipo visual original y no sustituyen esta guía de ejecución conectada.

Limitaciones actuales para la prueba: el mapa y el escáner utilizan aplicaciones externas; las imágenes se seleccionan con Android; el contador manual no sustituye un sensor físico. La recuperación del turno activo depende de su asociación local: borrar los datos de la aplicación elimina esa asociación y el backend actual no ofrece una consulta para reconstruirla automáticamente.
