package com.romaneio.romaneio.service;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.regex.*;

@Service
public class RomaneioService {

    public byte[] gerarRomaneio(List<MultipartFile> files, String transportadora, InputStream logoStream) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        PDType1Font fontBold = PDType1Font.HELVETICA_BOLD;
        PDType1Font fontRegular = PDType1Font.HELVETICA;

        float margin = 50;
        float yStart = page.getMediaBox().getHeight() - margin;

        // Logo
        if (logoStream != null) {
            PDImageXObject logo = PDImageXObject.createFromByteArray(document, logoStream.readAllBytes(), "logo");
            contentStream.drawImage(logo, margin, yStart - 60, 80, 40);
        }

        // Título
        contentStream.beginText();
        contentStream.setFont(fontBold, 18);
        float titleX = page.getMediaBox().getWidth() / 2 - 80;
        float titleY = yStart - 50;
        contentStream.newLineAtOffset(titleX, titleY);
        contentStream.showText("ROMANEIO DE CARGA");
        contentStream.endText();

        // Data atual no topo direito
        String dataAtual = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        contentStream.beginText();
        contentStream.setFont(fontRegular, 12);
        float dataWidth = fontRegular.getStringWidth(dataAtual) / 1000 * 12;
        float dataX = page.getMediaBox().getWidth() - margin - dataWidth;
        contentStream.newLineAtOffset(dataX, titleY);
        contentStream.showText(dataAtual);
        contentStream.endText();

        // Transportadora com destaque
        float y = yStart - 100;
        contentStream.beginText();
        contentStream.setFont(fontBold, 14);
        contentStream.newLineAtOffset(margin, y);
        contentStream.showText("Transportadora: " + transportadora);
        contentStream.endText();

        y -= 30;

        // Tabela de dados
        List<String[]> linhas = new ArrayList<>();
        int totalNotas = 0;
        int totalVolumes = 0;

        for (MultipartFile file : files) {
            try (InputStream inputStream = file.getInputStream()) {
                PDDocument pdfDoc = PDDocument.load(inputStream);
                String text = new PDFTextStripper().getText(pdfDoc);

                String numero = extrair(text, "N°\\s*(\\d{3}\\.\\d{3})");
                String razao = extrair(text, "ENTRADA/SAÍDACEP\\s+([A-ZÀ-Ÿ\\s]+LTDA)");
                String municipio = extrair(text, "\\d{4}\\s+\\d{4}\\s+\\d{4}\\s+\\d{4}\\s+\\d{4}\\s+\\d{4}\\s+1\\s+([A-Za-zÀ-ÿ]+)");
                String volumesStr = extrair(text, "\\b(\\d+,\\d{2})\\b");
                String valorTotal = extrair(text, "VALOR TOTAL IPI\\s+[\\d.,]+\\s+[\\d.,]+\\s+[\\d.,]+\\s+(\\d+,\\d{2})");

                int volumes = 0;
                try {
                    volumes = Integer.parseInt(volumesStr.split(",")[0]);
                } catch (Exception ignored) {}

                totalVolumes += volumes;
                totalNotas++;

                linhas.add(new String[]{numero, razao, municipio, String.valueOf(volumes), valorTotal});
                pdfDoc.close();
            }
        }

        // Cabeçalho da tabela
        float rowHeight = 20;
        float tableWidth = 500;
        float[] colWidths = {80, 180, 100, 60, 80};
        String[] headers = {"Nº NFe", "Razão Social", "Município", "Volumes", "Valor Total"};

        float tableX = margin;
        float tableY = y;

        contentStream.setStrokingColor(0, 0, 0); // borda preta
        contentStream.setNonStrokingColor(220, 220, 220); // cinza claro para o cabeçalho

        // Fundo do cabeçalho
        contentStream.addRect(tableX, tableY - rowHeight, tableWidth, rowHeight);
        contentStream.fill();

        // Borda do cabeçalho
        contentStream.setStrokingColor(0, 0, 0);
        contentStream.addRect(tableX, tableY - rowHeight, tableWidth, rowHeight);
        contentStream.stroke();

        // Texto do cabeçalho (centralizado)
        float textY = tableY - 15;
        float nextX = tableX;
        contentStream.setNonStrokingColor(0, 0, 0); // texto preto
        contentStream.setFont(fontBold, 11);
        for (int i = 0; i < headers.length; i++) {
            float textWidth = fontBold.getStringWidth(headers[i]) / 1000 * 11;
            float textX = nextX + (colWidths[i] - textWidth) / 2;
            contentStream.beginText();
            contentStream.newLineAtOffset(textX, textY);
            contentStream.showText(headers[i]);
            contentStream.endText();
            nextX += colWidths[i];
        }

        // Linhas da tabela
        contentStream.setFont(fontRegular, 10);
        y = tableY - rowHeight;
        for (String[] linha : linhas) {
            y -= rowHeight;
            nextX = tableX;

            for (int i = 0; i < linha.length; i++) {
                String texto = linha[i] != null ? linha[i] : "";
                float textWidth = fontRegular.getStringWidth(texto) / 1000 * 10;
                float textX = nextX + (colWidths[i] - textWidth) / 2;

                // Texto da célula
                contentStream.beginText();
                contentStream.newLineAtOffset(textX, y + 5);
                contentStream.showText(texto);
                contentStream.endText();

                // Linha separadora vertical entre colunas
                contentStream.moveTo(nextX, y);
                contentStream.lineTo(nextX, y + rowHeight);
                contentStream.stroke();

                nextX += colWidths[i];
            }

            // Linha horizontal final da linha
            contentStream.moveTo(tableX, y);
            contentStream.lineTo(tableX + tableWidth, y);
            contentStream.stroke();
        }

        // Linha vertical direita para fechar a tabela
        contentStream.moveTo(tableX + tableWidth, y);
        contentStream.lineTo(tableX + tableWidth, y + rowHeight * (linhas.size() + 1));
        contentStream.stroke();

        y -= 30;

        // Totais centralizados abaixo da tabela
        contentStream.setFont(fontBold, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, y);
        contentStream.showText("Total de Notas: " + totalNotas + "    |    Total de Volumes: " + totalVolumes);
        contentStream.endText();

        y -= 40;

        // Rodapé
        contentStream.setFont(fontRegular, 10);
        contentStream.setNonStrokingColor(100, 100, 100);
        contentStream.beginText();
        contentStream.newLineAtOffset(margin, 50);
        contentStream.showText("Documento gerado por Flvx Hidro © 2025. Todos os direitos reservados.");
        contentStream.endText();

        contentStream.close();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        document.save(output);
        document.close();
        return output.toByteArray();
    }

    private String extrair(String texto, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(texto);
        return matcher.find() ? matcher.group(1).trim() : "";
    }
}
