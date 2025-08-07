package zeco.suza.eoreporterv1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/geolocation")
@CrossOrigin(origins = "*")
public class GeoLocationController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/reverse-geocode")
    public ResponseEntity<Map<String, Object>> reverseGeocode(
            @RequestParam Double lat,
            @RequestParam Double lon) {
        
        try {
            String nominatimUrl = String.format(
                "https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&zoom=18&addressdetails=1",
                lat, lon
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "EOReporter/1.0");
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                nominatimUrl,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response.getBody());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to get location: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
