package com.recyops.api.entrega.service;

import com.recyops.api.entrega.entity.Entrega;
import com.recyops.api.entrega.excepciones.ErrorGeneracionReciboException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

/**
 * Renderiza el recibo PDF de una entrega. Concentra fuentes, coordenadas
 * y saneamiento de texto para que el service de entregas no conozca PDFBox.
 */
@Component
public class GeneradorReciboPdf {

    // Las fuentes Standard-14 son inmutables y compartibles entre documentos.
    private static final PDType1Font NEGRITA = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font NORMAL = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final float MARGEN = 50f;
    private static final float ALTO_FILA = 24f;
    private static final float SANGRIA_VALOR = 160f;

    public byte[] generar(Entrega entrega) {
        try {
            return construir(entrega);
        } catch (IOException e) {
            throw new ErrorGeneracionReciboException(entrega.getCodigo(), e);
        }
    }

    private byte[] construir(Entrega entrega) throws IOException {
        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            float derecha = pagina.getMediaBox().getWidth() - MARGEN;
            float y = pagina.getMediaBox().getHeight() - MARGEN;

            try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                // Encabezado del sistema
                y -= 12;
                escribir(contenido, NEGRITA, 20, MARGEN, y, "RecyOPS");
                y -= 16;
                escribir(contenido, NORMAL, 10, MARGEN, y, "Sistema de gestion de operaciones de reciclaje");
                y -= 36;
                escribir(contenido, NEGRITA, 14, MARGEN, y, "Recibo de entrega " + entrega.getCodigo());
                y -= 12;
                linea(contenido, MARGEN, y, derecha, y);
                y -= 30;

                // Datos de la entrega
                for (Map.Entry<String, String> fila : filasRecibo(entrega).entrySet()) {
                    String valor = fila.getValue();
                    escribir(contenido, NEGRITA, 11, MARGEN, y, fila.getKey());
                    escribir(contenido, NORMAL, 11, MARGEN + SANGRIA_VALOR, y,
                            valor != null && !valor.isBlank() ? valor : "No registrado");
                    y -= ALTO_FILA;
                }

                // Pie con fecha de generacion
                linea(contenido, MARGEN, y - 8, derecha, y - 8);
                escribir(contenido, NORMAL, 9, MARGEN, y - 24,
                        "Generado por RecyOPS el " + LocalDateTime.now().format(FORMATO_FECHA));
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    /** Filas etiqueta/valor del recibo, en el orden en que se imprimen. */
    private Map<String, String> filasRecibo(Entrega entrega) {
        Map<String, String> filas = new LinkedHashMap<>();
        filas.put("Codigo", entrega.getCodigo());
        filas.put("Proveedor", entrega.getProveedor().getNombre());
        filas.put("Bodega", entrega.getBodega().getNombre());
        filas.put("Tipo de material", entrega.getTipoMaterial().getNombre());
        filas.put("Peso", entrega.getPesoKg().toPlainString() + " kg");
        filas.put("Persona que entrega", entrega.getPersonaEntrega());
        filas.put("Estado", entrega.getEstado().name().replace('_', ' '));
        filas.put("Fecha de recepcion", entrega.getFechaRecepcion().format(FORMATO_FECHA));
        filas.put("Registrado por", entrega.getUsuarioRegistroNombre());
        return filas;
    }

    private void escribir(PDPageContentStream contenido, PDType1Font fuente, float tamano,
            float x, float y, String texto) throws IOException {
        contenido.beginText();
        contenido.setFont(fuente, tamano);
        contenido.newLineAtOffset(x, y);
        contenido.showText(sanitizar(fuente, texto));
        contenido.endText();
    }

    /**
     * Las fuentes Standard-14 solo codifican WinAnsi: un caracter fuera de ese
     * rango (emoji, comillas tipograficas) o de control haria que showText
     * lance IllegalArgumentException, asi que se reemplaza antes de escribir.
     */
    private String sanitizar(PDType1Font fuente, String texto) {
        StringBuilder limpio = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (Character.isISOControl(c)) {
                limpio.append(' ');
                continue;
            }
            try {
                fuente.encode(String.valueOf(c));
                limpio.append(c);
            } catch (IOException | IllegalArgumentException e) {
                limpio.append('?');
            }
        }
        return limpio.toString();
    }

    private void linea(PDPageContentStream contenido, float x1, float y1, float x2, float y2) throws IOException {
        contenido.moveTo(x1, y1);
        contenido.lineTo(x2, y2);
        contenido.stroke();
    }
}
