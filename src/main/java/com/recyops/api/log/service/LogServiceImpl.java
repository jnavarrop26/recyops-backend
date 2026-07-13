package com.recyops.api.log.service;

import com.recyops.api.log.dtos.RespuestaLineaLog;
import com.recyops.api.log.enums.TipoArchivoLog;
import com.recyops.api.log.interfaces.LogService;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Lee los archivos que escribe logback (ver logback-spring.xml) y los parsea
 * al formato estructurado que consume la vista de Logs. Para no cargar
 * archivos de 20 MB completos, solo se lee la cola del archivo.
 */
@Service
public class LogServiceImpl implements LogService {

    private static final Path CARPETA_LOGS = Path.of("logs");
    private static final int BYTES_MAXIMOS_LECTURA = 512 * 1024;

    // 2026-07-12 09:28:23.290 INFO  [hilo] [usuario=x|empresa=y] logger - mensaje
    private static final Pattern PATRON_LINEA = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) (\\w+)\\s+\\[[^\\]]*\\] "
                    + "\\[usuario=([^|]*)\\|empresa=([^\\]]*)\\] (\\S+) - (.*)$");

    @Override
    public List<RespuestaLineaLog> leer(TipoArchivoLog archivo, int lineas) {
        Path ruta = CARPETA_LOGS.resolve(archivo.getNombreArchivo());
        if (!Files.exists(ruta)) {
            return List.of();
        }

        List<RespuestaLineaLog> entradas = new ArrayList<>();
        RespuestaLineaLog actual = null;
        StringBuilder detalle = new StringBuilder();

        for (String linea : leerCola(ruta)) {
            Matcher coincidencia = PATRON_LINEA.matcher(linea);
            if (coincidencia.matches()) {
                agregar(entradas, actual, detalle);
                detalle.setLength(0);
                actual = new RespuestaLineaLog(
                        coincidencia.group(1),
                        coincidencia.group(2),
                        coincidencia.group(3),
                        coincidencia.group(4),
                        coincidencia.group(5),
                        coincidencia.group(6),
                        null);
            } else if (actual != null && !linea.isBlank()) {
                // Linea de continuacion (stack trace) de la entrada anterior
                if (!detalle.isEmpty()) {
                    detalle.append('\n');
                }
                detalle.append(linea);
            }
        }
        agregar(entradas, actual, detalle);

        // Mas recientes primero y recortado a lo pedido
        Collections.reverse(entradas);
        return entradas.size() > lineas ? entradas.subList(0, lineas) : entradas;
    }

    private void agregar(List<RespuestaLineaLog> entradas, RespuestaLineaLog entrada, StringBuilder detalle) {
        if (entrada == null) {
            return;
        }
        entradas.add(detalle.isEmpty() ? entrada
                : new RespuestaLineaLog(entrada.fecha(), entrada.nivel(), entrada.usuario(),
                        entrada.empresa(), entrada.logger(), entrada.mensaje(), detalle.toString()));
    }

    /** Ultimos BYTES_MAXIMOS_LECTURA del archivo, partidos en lineas. */
    private List<String> leerCola(Path ruta) {
        try (RandomAccessFile acceso = new RandomAccessFile(ruta.toFile(), "r")) {
            long tamano = acceso.length();
            long inicio = Math.max(0, tamano - BYTES_MAXIMOS_LECTURA);
            acceso.seek(inicio);
            byte[] contenido = new byte[(int) (tamano - inicio)];
            acceso.readFully(contenido);
            String texto = new String(contenido, StandardCharsets.UTF_8);
            List<String> resultado = new ArrayList<>(List.of(texto.split("\r?\n", -1)));
            // Si se empezo a leer a mitad de archivo, la primera linea esta cortada
            if (inicio > 0 && !resultado.isEmpty()) {
                resultado.remove(0);
            }
            return resultado;
        } catch (IOException ex) {
            return List.of();
        }
    }
}
