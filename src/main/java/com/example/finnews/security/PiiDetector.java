package com.example.finnews.security;

public class PiiDetector {
    public String anonymize(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", "[EMAIL]")
                .replaceAll("\\b\\d{3}-\\d{2}-\\d{4}\\b", "[SSN]")
                .replaceAll("\\b(?:\\+?1[-. ]?)?\\(?\\d{3}\\)?[-. ]?\\d{3}[-. ]?\\d{4}\\b", "[PHONE]");
    }
}
