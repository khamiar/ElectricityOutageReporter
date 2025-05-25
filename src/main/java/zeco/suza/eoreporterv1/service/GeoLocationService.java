package zeco.suza.eoreporterv1.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GeoLocationService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String reverseGeocode(double lat, double lon) {
        String url = String.format(
                "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%f&lon=%f",
                lat, lon
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "ZECO-EOR-App");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getBody() != null && response.getBody().get("display_name") != null) {
                return response.getBody().get("display_name").toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Unknown Location";
    }
}
