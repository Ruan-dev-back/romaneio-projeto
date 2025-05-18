package com.romaneio.romaneio.controller;

import com.romaneio.romaneio.service.RomaneioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@CrossOrigin
public class RomaneioController {  // <-- Remova @RequestMapping("/api")

    @Autowired
    private RomaneioService romaneioService;

    @PostMapping("/gerarRomaneio")
    public ResponseEntity<byte[]> gerarRomaneio(@RequestParam("files") List<MultipartFile> files,
                                                @RequestParam("transportadora") String transportadora) {
        try {
            InputStream logoStream = new ClassPathResource("static/img/logo.png").getInputStream();
            byte[] romaneioPdf = romaneioService.gerarRomaneio(files, transportadora, logoStream);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "romaneio.pdf");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(romaneioPdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

