# Validación

Validación del prototipo de los tres segmentos en Pixel 5, Android 13 / API 33. Target SDK 35, Compile SDK 37.

## Cobertura

- **Conductor (7 pruebas):** credenciales inválidas, asignación, QR válido/inválido, emergencia con un toque, acceso global durante el turno, desconexión y recuperación, atención/cierre, estados vacíos, recreación de actividad, idioma y cierre de sesión/turno.
- **Acceso compartido:** ninguna de las tres cuentas demo ni su contraseña aparece en el formulario.
- **Pasajero:** navegación exclusiva de su rol; registro con DNI duplicado, foto y términos; QR y viaje; bloqueo de solicitudes sin mensaje/foto; evidencia inválida; revisión/envío; resúmenes públicos sin evidencia ajena; desconexión/recuperación; estados de aprobación/cierre; fin de viaje manual o simulado y seguimiento posterior.
- **Supervisor:** búsqueda y filtros de flota, selección en mapa, respaldo en lista, capacidad positiva, recreación de actividad, atención y resultado obligatorio, historial, revisión de evidencia, aprobación High, rechazo con motivo sin crear emergencia, conflicto de asignación y cambio de idioma.

## Resultados

| Comprobación | Resultado |
|---|---|
| `assembleDebug` y `assembleDebugAndroidTest` | APK de aplicación y pruebas generados correctamente. |
| `lintDebug` | 0 errores; 5 avisos informativos de versiones de dependencias y Target SDK 35. |
| Tema claro, texto 100 % | **16 pruebas, 0 fallos**: 7 del conductor y 9 del acceso/pasajero/supervisor. |
| Tema oscuro, texto 200 % | **4 pruebas, 0 fallos**: solicitud y fin de viaje, registro, aprobación y evidencia, flota/mapa/capacidad. |
| Recursos EN/ES | Mismas claves de traducción en ambos idiomas. |
| Permisos | El manifiesto no declara permisos de cámara, ubicación, llamadas ni Internet. |

Los registros instrumentados están en [test-results](test-results). La comprobación con texto ampliado se repitió tras ajustar el contraste de campos y acciones de diálogo. Los controles mantienen la paleta del informe; la navegación del supervisor distribuye el ancho según la longitud de cada etiqueta, sin reducir sus 14 sp.

Las capturas se conservan en [screenshots/light](screenshots/light) y [screenshots/dark-large-text](screenshots/dark-large-text). `screenshots/dark` y las capturas del conductor con fuente ampliada corresponden a la validación previa del primer segmento. Las capturas muestran el desplazamiento y teclado que estaban presentes en cada paso; las acciones fuera del área visible siguen accesibles mediante desplazamiento. El emulador presentó inicialmente un aviso de System UI; se restableció su interfaz y se repitieron las pruebas/capturas sin el aviso.

No se ejecutó en un dispositivo API 34/35 ni se verificaron servicios reales, fuera del alcance de UI pura. El mapa y la evidencia son representaciones locales; no se evalúan permisos reales, GPS, captura fotográfica, cartografía, autenticación, sincronización entre cuentas o persistencia de negocio.

## Repetir

```powershell
./gradlew.bat assembleDebug lintDebug connectedDebugAndroidTest
```

Para conservar la aplicación instalada y recuperar capturas con Android Studio Device Explorer, también puede ejecutar el runner directamente:

```powershell
./gradlew.bat assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r com.safebus.driver.test/androidx.test.runner.AndroidJUnitRunner
```

Las pruebas guardan PNG en `files/screenshots` dentro del directorio privado de la aplicación. Para la comprobación ampliada active tema oscuro y fuente al 200 % en el emulador de prueba. Restaure los ajustes al terminar.