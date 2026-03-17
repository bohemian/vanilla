package vanilla;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@UtilityClass
public class Utils {
    String toSentenceCase(Enum<?> enumInstance) {
        String name = enumInstance.name();
        return name.charAt(0) + name.substring(1).replace("_", " ").toLowerCase();
    }

    public List<String> decodeJWT(String jwt) {
        try {
            // Split the JWT into its three parts
            String[] parts = jwt.split("\\.");

            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }

            // Decode each part
            String header = decodeBase64(parts[0]);
            String payload = decodeBase64(parts[1]);
            String signature = parts[2];

            return List.of(header, payload, signature);

        } catch (Exception e) {
            throw new RuntimeException("Error decoding JWT: " + e.getMessage());
        }
    }

    private String decodeBase64(String base64) {
        // JWT uses Base64 URL encoding, so we need to convert it to standard Base64
        String base64Url = base64.replace('-', '+').replace('_', '/');

        // Add padding if needed
        int padding = (4 - base64Url.length() % 4) % 4;
        for (int i = 0; i < padding; i++) {
            base64Url += "=";
        }

        byte[] decoded = Base64.getDecoder().decode(base64Url);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    public static String encodeJWT(String header, String payload, String signature) {
        try {
            // Encode header and payload to Base64 URL format
            String encodedHeader = encodeBase64(header);
            String encodedPayload = encodeBase64(payload);

            // Combine all three parts with dots
            return encodedHeader + "." + encodedPayload + "." + signature;

        } catch (Exception e) {
            throw new RuntimeException("Error encoding JWT: " + e.getMessage());
        }
    }

    private static String encodeBase64(String text) {
        // Encode to standard Base64
        byte[] encoded = Base64.getEncoder().encode(text.getBytes(StandardCharsets.UTF_8));
        String base64 = new String(encoded, StandardCharsets.UTF_8);

        // Convert to Base64 URL format (JWT standard)
        return base64.replace('+', '-')
                .replace('/', '_');
    }
}