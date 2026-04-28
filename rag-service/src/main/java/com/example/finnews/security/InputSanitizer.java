package com.example.finnews.security;

public class InputSanitizer {
    public String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
                .replaceAll("<script.*?>.*?</script>", "")
                .trim();
    }
}
