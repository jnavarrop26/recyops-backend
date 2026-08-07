package com.recyops.api.unit.log.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.recyops.api.log.dtos.RespuestaLineaLog;
import com.recyops.api.log.enums.TipoArchivoLog;
import com.recyops.api.log.service.LogServiceImpl;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * LogServiceImpl no recibe sus dependencias por inyeccion: la carpeta "logs"
 * esta fija dentro de la clase. Para probar el parseo real sin acoplarse a lo
 * que haya en los logs de desarrollo, cada prueba respalda el contenido
 * existente del archivo objetivo antes de escribir contenido de prueba y lo
 * restaura en un bloque finally, incluso si la aserción falla.
 */
class LogServiceImplTest {

    private static final Path RUTA_LOG = Path.of("logs", TipoArchivoLog.GENERAL.getNombreArchivo());

    private final LogServiceImpl logService = new LogServiceImpl();

    @Test
    void leer_archivoNoExiste_retornaListaVacia() throws IOException {
        // Given
        byte[] respaldo = respaldar();
        try {
            Files.deleteIfExists(RUTA_LOG);

            // When
            List<RespuestaLineaLog> actualEntradas = logService.leer(TipoArchivoLog.GENERAL, 10);

            // Then
            assertThat(actualEntradas).isEmpty();
        } finally {
            restaurar(respaldo);
        }
    }

    @Test
    void leer_lineasBienFormadas_retornaEntradasParseadasDeLaMasRecienteALaMasAntigua() throws IOException {
        // Given
        byte[] respaldo = respaldar();
        try {
            String lineaA = "2026-01-15 09:28:23.290 INFO  [hilo-1] [usuario=jnavarro|empresa=empresa_demo] "
                    + "com.recyops.LoggerA - Primer mensaje";
            String lineaB = "2026-01-15 09:29:10.500 WARN  [hilo-2] [usuario=jnavarro|empresa=empresa_demo] "
                    + "com.recyops.LoggerB - Segundo mensaje";
            escribir(lineaA + "\n" + lineaB);

            // When
            List<RespuestaLineaLog> actualEntradas = logService.leer(TipoArchivoLog.GENERAL, 10);

            // Then
            assertThat(actualEntradas).hasSize(2);
            RespuestaLineaLog masReciente = actualEntradas.get(0);
            assertThat(masReciente.fecha()).isEqualTo("2026-01-15 09:29:10.500");
            assertThat(masReciente.nivel()).isEqualTo("WARN");
            assertThat(masReciente.usuario()).isEqualTo("jnavarro");
            assertThat(masReciente.empresa()).isEqualTo("empresa_demo");
            assertThat(masReciente.mensaje()).isEqualTo("Segundo mensaje");
            assertThat(masReciente.detalle()).isNull();
            assertThat(actualEntradas.get(1).mensaje()).isEqualTo("Primer mensaje");
        } finally {
            restaurar(respaldo);
        }
    }

    @Test
    void leer_lineaConLineasDeContinuacion_incluyeDetalleConElStackTrace() throws IOException {
        // Given
        byte[] respaldo = respaldar();
        try {
            String lineaBase = "2026-01-15 10:00:00.000 ERROR [hilo-3] [usuario=jnavarro|empresa=empresa_demo] "
                    + "com.recyops.LoggerC - Fallo inesperado";
            String continuacion1 = "\tat com.recyops.Foo.bar(Foo.java:10)";
            String continuacion2 = "\tat com.recyops.Baz.qux(Baz.java:20)";
            escribir(lineaBase + "\n" + continuacion1 + "\n" + continuacion2);

            // When
            List<RespuestaLineaLog> actualEntradas = logService.leer(TipoArchivoLog.GENERAL, 10);

            // Then
            assertThat(actualEntradas).hasSize(1);
            assertThat(actualEntradas.get(0).detalle()).isEqualTo(continuacion1 + "\n" + continuacion2);
        } finally {
            restaurar(respaldo);
        }
    }

    @Test
    void leer_masEntradasQueElLimite_recortaAlLimiteSolicitado() throws IOException {
        // Given
        byte[] respaldo = respaldar();
        try {
            String lineaC = "2026-01-15 11:00:00.000 INFO  [hilo-1] [usuario=jnavarro|empresa=empresa_demo] "
                    + "com.recyops.LoggerC - Mensaje C";
            String lineaD = "2026-01-15 11:01:00.000 INFO  [hilo-1] [usuario=jnavarro|empresa=empresa_demo] "
                    + "com.recyops.LoggerD - Mensaje D";
            String lineaE = "2026-01-15 11:02:00.000 INFO  [hilo-1] [usuario=jnavarro|empresa=empresa_demo] "
                    + "com.recyops.LoggerE - Mensaje E";
            escribir(lineaC + "\n" + lineaD + "\n" + lineaE);

            // When
            List<RespuestaLineaLog> actualEntradas = logService.leer(TipoArchivoLog.GENERAL, 2);

            // Then
            assertThat(actualEntradas).hasSize(2);
            assertThat(actualEntradas.get(0).mensaje()).isEqualTo("Mensaje E");
            assertThat(actualEntradas.get(1).mensaje()).isEqualTo("Mensaje D");
        } finally {
            restaurar(respaldo);
        }
    }

    @Test
    void leer_lineaSinFormatoAntesDeLaPrimeraEntrada_seIgnora() throws IOException {
        // Given
        byte[] respaldo = respaldar();
        try {
            String lineaInvalida = "esto no tiene el formato esperado de una linea de log";
            String lineaValida = "2026-01-15 12:00:00.000 INFO  [hilo-1] [usuario=jnavarro|empresa=empresa_demo] "
                    + "com.recyops.LoggerF - Mensaje valido";
            escribir(lineaInvalida + "\n" + lineaValida);

            // When
            List<RespuestaLineaLog> actualEntradas = logService.leer(TipoArchivoLog.GENERAL, 10);

            // Then
            assertThat(actualEntradas).hasSize(1);
            assertThat(actualEntradas.get(0).mensaje()).isEqualTo("Mensaje valido");
        } finally {
            restaurar(respaldo);
        }
    }

    private void escribir(String contenido) throws IOException {
        Files.createDirectories(RUTA_LOG.getParent());
        Files.writeString(RUTA_LOG, contenido, StandardCharsets.UTF_8);
    }

    /** Contenido original del archivo, o null si el archivo no existia. */
    private byte[] respaldar() throws IOException {
        return Files.exists(RUTA_LOG) ? Files.readAllBytes(RUTA_LOG) : null;
    }

    private void restaurar(byte[] respaldo) throws IOException {
        if (respaldo == null) {
            Files.deleteIfExists(RUTA_LOG);
        } else {
            Files.write(RUTA_LOG, respaldo);
        }
    }
}
