package com.bentiss.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class EmployeeController {
    private int method_code;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, List<String>> allPrayerTimesFajr = new HashMap<>();
    private Map<String, List<String>> allPrayerTimesSunrise = new HashMap<>();
    private Map<String, List<String>> allPrayerTimesDhuhr = new HashMap<>();
    private Map<String, List<String>> allPrayerTimesAsr = new HashMap<>();
    private Map<String, List<String>> allPrayerTimesMaghrib = new HashMap<>();
    private Map<String, List<String>> allPrayerTimesIsha = new HashMap<>();

    @GetMapping("/v1/json")
    @PostMapping("/v1/json")
    public ResponseEntity<String> getPrayerTimes(
            @RequestParam String latitude,
            @RequestParam String longitude
    ) throws JsonProcessingException {
        int years = 3;
        Year currentYear = Year.now();
        Map<String, String> locationInfo = getLocationInfo(latitude, longitude);

        for (int i = 0; i < years; i++) {
            Year targetYear = currentYear.plusYears(i);
            String apiUrl = String.format("https://api.aladhan.com/v1/calendar/%s?latitude=%s&longitude=%s&method=%s", targetYear, latitude, longitude, method_code);
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(apiUrl, String.class);

            try {
                JsonNode dataNode = objectMapper.readTree(response).path("data");

                List<String> prayerTimesForYearFajr = new ArrayList<>();
                List<String> prayerTimesForYearSunrise = new ArrayList<>();
                List<String> prayerTimesForYearDhuhr = new ArrayList<>();
                List<String> prayerTimesForYearAsr = new ArrayList<>();
                List<String> prayerTimesForYearMaghrib = new ArrayList<>();
                List<String> prayerTimesForYearIsha = new ArrayList<>();

                for (int month = 1; month <= dataNode.size(); month++) {
                    JsonNode monthNode = dataNode.path(String.valueOf(month));

                    for (JsonNode dayNode : monthNode) {
                        JsonNode timingsNode = dayNode.path("timings");

                        prayerTimesForYearFajr.add(getTimeOnly(timingsNode.path("Fajr").asText()));
                        prayerTimesForYearSunrise.add(getTimeOnly(timingsNode.path("Sunrise").asText()));
                        prayerTimesForYearDhuhr.add(getTimeOnly(timingsNode.path("Dhuhr").asText()));
                        prayerTimesForYearAsr.add(getTimeOnly(timingsNode.path("Asr").asText()));
                        prayerTimesForYearMaghrib.add(getTimeOnly(timingsNode.path("Maghrib").asText()));
                        prayerTimesForYearIsha.add(getTimeOnly(timingsNode.path("Isha").asText()));
                    }
                }

                allPrayerTimesFajr.put(i + "", prayerTimesForYearFajr);
                allPrayerTimesSunrise.put(i + "", prayerTimesForYearSunrise);
                allPrayerTimesDhuhr.put(i + "", prayerTimesForYearDhuhr);
                allPrayerTimesAsr.put(i + "", prayerTimesForYearAsr);
                allPrayerTimesMaghrib.put(i + "", prayerTimesForYearMaghrib);
                allPrayerTimesIsha.put(i + "", prayerTimesForYearIsha);

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error extracting prayer times: " + e.getMessage());
            }
        }


        StringBuilder resultBuilder = new StringBuilder("{\n");
        resultBuilder.append("  \"location\": ").append(objectMapper.writeValueAsString(locationInfo)).append(",\n");
        resultBuilder.append("  \"allPrayerTimesFajr\": ").append(objectMapper.writeValueAsString(allPrayerTimesFajr)).append(",\n");
        resultBuilder.append("   \"allPrayerTimesSunrise\": ").append(objectMapper.writeValueAsString(allPrayerTimesSunrise)).append(",\n");
        resultBuilder.append("   \"allPrayerTimesDhuhr\": ").append(objectMapper.writeValueAsString(allPrayerTimesDhuhr)).append(",\n");
        resultBuilder.append("   \"allPrayerTimesAsr\": ").append(objectMapper.writeValueAsString(allPrayerTimesAsr)).append(",\n");
        resultBuilder.append("   \"allPrayerTimesMaghrib\": ").append(objectMapper.writeValueAsString(allPrayerTimesMaghrib)).append(",\n");
        resultBuilder.append("   \"allPrayerTimesIsha\": ").append(objectMapper.writeValueAsString(allPrayerTimesIsha)).append("\n  }\n");

        return ResponseEntity.ok(resultBuilder.toString());
    }

    private String getTimeOnly(String dateTimeWithOffset) {
        // Extract time part only, assuming the format is always "HH:mm (+/-offset)"
        return dateTimeWithOffset.split(" ")[0];
    }

    private Map<String, String> getLocationInfo(String latitude, String longitude) {
        String locationApiUrlAr = String.format("https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=%s&longitude=%s&localityLanguage=ar", latitude, longitude);
        String locationResponseAr = new RestTemplate().getForObject(locationApiUrlAr, String.class);

        String locationApiUrlEn = String.format("https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=%s&longitude=%s&localityLanguage=en", latitude, longitude);
        String locationResponseEn = new RestTemplate().getForObject(locationApiUrlEn, String.class);

        Map<String, String> locationInfo = new HashMap<>();
        try {
            JsonNode locationNodeAr = objectMapper.readTree(locationResponseAr);
            locationInfo.put("cityAr", locationNodeAr.path("city").asText());
            locationInfo.put("countryNameAr", locationNodeAr.path("countryName").asText());

            JsonNode locationNodeEn = objectMapper.readTree(locationResponseEn);
            locationInfo.put("cityEn", locationNodeEn.path("city").asText());
            locationInfo.put("countryNameEn", locationNodeEn.path("countryName").asText());
            locationInfo.put("countryCode", locationNodeEn.path("countryCode").asText());

            code(locationNodeEn.path("countryCode")+"");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return locationInfo;
    }
    private void code(String code){
        switch (code) {
            case "US":
            case "CA":
            case "MX":
                method_code = 2;
                break;
            case "EG":
                method_code = 3;
                break;
            case "SA":
                method_code = 4;
                break;
            case "PK":
                method_code = 5;
                break;
            case "IR":
                method_code = 6;
                break;
            case "BH":
            case "OM":
                method_code = 8;
                break;
            case "KW":
                method_code = 9;
                break;
            case "QA":
                method_code = 10;
                break;
            case "SG":
                method_code = 11;
                break;
            case "FR":
                method_code = 12;
                break;
            case "TR":
                method_code = 13;
                break;
            case "RU":
                method_code = 14;
                break;
            case "AE":
                method_code = 14;
                break;
            case "MA":
                method_code = 21;
                break;
            default:
                method_code = 5;
                break;
        }

    }
}
