package com.romaneio.romaneio.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.romaneio.romaneio.service.RomaneioService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class RomaneioController {

    @Autowired
    private RomaneioService romaneioService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/gerarRomaneio")
    public ResponseEntity<byte[]> gerarRomaneio(@RequestParam("arquivoNota") MultipartFile arquivoNota) {
        try {
            // Extrair texto do PDF da nota
            String textoPdf = romaneioService.extrairTextoDoPdf(arquivoNota.getInputStream());
            System.out.println(textoPdf); // Adicione isso aqui!


            // Extrair informações específicas
            String numeroNota = romaneioService.extrairNumeroNota(textoPdf);
            String descricao = romaneioService.extrairDescricao(textoPdf);

            // Gerar o romaneio em PDF
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 18);
                    contentStream.newLineAtOffset(100, 750);
                    contentStream.showText("Romaneio");
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
                    contentStream.newLineAtOffset(100, 700);
                    contentStream.showText(("Número da Nota: " + numeroNota).replaceAll("[\\r\\n]+", " "));
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(100, 680);
                    contentStream.showText(("Descrição: " + descricao).replaceAll("[\\r\\n]+", " "));
                    contentStream.endText();
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                document.save(baos);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", "romaneio.pdf");

                return ResponseEntity.ok().headers(headers).body(baos.toByteArray());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
