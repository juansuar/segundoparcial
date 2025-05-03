package com.mycompany.covidstatsapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mycompany.covidstatsapp.model.Report;
import com.mycompany.covidstatsapp.repository.ReportRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

@Log4j2
public class ReportService {

    private final ReportRepository reportRepository;
    private final String baseUrl;
    private final String apiKey;
    private final String apiHost;
    private final String iso;
    private final String reportDate;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;

        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
        } catch (IOException e) {
            log.error("Error al leer application.properties", e);
        }

        this.baseUrl = props.getProperty("api.base.url");
        this.apiKey = props.getProperty("api.key");
        this.apiHost = props.getProperty("api.host");
        this.iso = props.getProperty("api.country.iso");
        this.reportDate = props.getProperty("api.date");

        // Validamos si los datos criticos son nulos o vacios
        if (this.iso == null || this.iso.isEmpty()) {
            log.error("La propiedad 'api.country.iso' no esta definida en application.properties o esta vacia.");
            throw new IllegalArgumentException("La propiedad 'api.country.iso' es obligatoria y no esta definida.");
        }

        if (this.reportDate == null || this.reportDate.isEmpty()) {
            log.error("La propiedad 'api.date' no esta definida en application.properties o esta vacia.");
            throw new IllegalArgumentException("La propiedad 'api.date' es obligatoria y no esta definida.");
        }
    }

    public void fetchAndSaveReport() {
        try {
            // Validacion defensiva por si acaso
            if (iso == null || iso.isEmpty()) {
                log.error("El codigo ISO es nulo o vacío. Abortando fetch.");
                return;
            }
            if (reportDate == null || reportDate.isEmpty()) {
                log.error("La fecha del reporte es nula o vacia. Abortando ejecucion.");
                return;
            }

            String encodedIso = URLEncoder.encode(iso, StandardCharsets.UTF_8);
            String encodedDate = URLEncoder.encode(reportDate, StandardCharsets.UTF_8);
            String urlStr = String.format("%s/reports?iso=%s&date=%s", baseUrl, encodedIso, encodedDate);

            log.info("Solicitando datos a la URL: " + urlStr);

            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-RapidAPI-Key", apiKey);
            connection.setRequestProperty("X-RapidAPI-Host", apiHost);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                log.info(" !!!Conexion exitosa a reports");
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder responseJson = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        responseJson.append(line);
                    }
                    parseAndSave(responseJson.toString());
                }
            } else {
                log.error(" Error en la respuesta reports: " + responseCode);
            }

        } catch (IOException e) {
            log.error("Error al consumir reports", e);
        }
    }

    private void parseAndSave(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode data = root.get("data");

            if (data == null || !data.isArray()) {
                log.warn("⚠ No se encontro un array 'data' en la respuesta JSON.");
                return;
            }

            for (JsonNode node : data) {
                JsonNode dateNode = node.get("date");
                String reportDatee = dateNode != null ? dateNode.asText() : "";

                JsonNode regionNode = node.get("region");
                String province = (regionNode.has("province") && !regionNode.get("province").isNull())
                        ? regionNode.get("province").asText()
                        : "N/A";

                if (reportRepository.existsByDateAndProvince(reportDatee, province)) {
                    log.info("Reporte ya existe para la fecha: " + reportDatee + " y provincia: " + province);
                    continue;
                }

                Report report = new Report();
                report.setDate(reportDatee);
                report.setConfirmed(node.get("confirmed").asInt());
                report.setDeaths(node.get("deaths").asInt());
                report.setRecovered(node.get("recovered").asInt());
                report.setActive(node.get("active").asInt());
                report.setIso(iso); // Guardamos el ISO
                report.setProvince(province); // Guardamos la provincia

                reportRepository.save(report);
                log.info(" Reporte guardado para fecha: " + report.getDate() + ", provincia: " + province);
            }

        } catch (JsonProcessingException e) {
            log.error(" Error al parsear el JSON de reportes", e);
        }
    }

    // NUEVO METODO: Consultar por Fecha e ISO y devolver un TreeMap agrupado por provincia
    public TreeMap<String, Report> consultarPorFechaYIso(String date, String iso) {
        List<Report> reportes = reportRepository.findByDateAndIso(date, iso);

        if (reportes.isEmpty()) {
            log.info(" No se encontraron reportes para la fecha {} y el ISO {}", date, iso);
            return new TreeMap<>();
        }

        TreeMap<String, Report> reportePorProvincia = new TreeMap<>();

        for (Report reporte : reportes) {
            String provincia = (reporte.getProvince() != null && !reporte.getProvince().isEmpty())
                    ? reporte.getProvince()
                    : "N/A";

            // Insertamos en el TreeMap (si hay duplicados por provincia se sobreescriben automaticamente)
            reportePorProvincia.put(provincia, reporte);
        }

        //  Imprimimos en formato tabla en consola
        System.out.println("---------------------------------------------------------------------------");
         System.out.printf("%-20s | %-12s | %-8s | %-13s | %-8s%n",
            "Provincia", "Confirmados", "Muertes", "Recuperados", "Activos");
         System.out.println("---------------------------------------------------------------------------");

            for (Report report : reportePorProvincia.values()) {
            System.out.printf("%-20s | %-12d | %-8d | %-13d | %-8d%n",
                report.getProvince(),
                report.getConfirmed(),
                report.getDeaths(),
                report.getRecovered(),
                report.getActive());
                     }

        System.out.println("---------------------------------------------------------------------------");

    return reportePorProvincia;
}
}
