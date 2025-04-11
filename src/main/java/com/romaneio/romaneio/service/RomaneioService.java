package com.romaneio.romaneio.service;

import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




@Service
public class RomaneioService {

    public String extrairTextoDoPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public String extrairNumeroNota(String texto) {
        Pattern pattern = Pattern.compile("Nota Fiscal(?:\\s*Nº| Número|:)?\\s*(\\d+)");
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Não encontrado";
    }


    public String extrairDescricao(String textoPdf) {
        int index = textoPdf.indexOf("Descrição:");
        if (index != -1) {
            return textoPdf.substring(index + 10, index + 50).trim(); // ajustar tamanho conforme seu PDF
        }
        return "Não encontrado";
    }
    public Map<String, String> extrairDadosDoPDF(MultipartFile arquivoNota) throws IOException {
        Map<String, String> dadosNota = new HashMap<>();

        try (PDDocument document = PDDocument.load(arquivoNota.getInputStream())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String texto = pdfStripper.getText(document);

            System.out.println("Conteúdo do PDF:\n" + texto);

            String numeroNota = texto.contains("Número da Nota:") ?
                    texto.split("Número da Nota:")[1].split("\\s+")[0] : "Não encontrado";

            String descricao = texto.contains("Descrição:") ?
                    texto.split("Descrição:")[1].split("\\n")[0] : "Não encontrado";

            dadosNota.put("numeroNota", numeroNota.trim());
            dadosNota.put("descricao", descricao.trim());
        }
        return dadosNota;
    }
}

