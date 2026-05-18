package com.example.finnews.service;

import com.example.finnews.model.CompanyProfile;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class CompanyProfileResolver {
    private static final Map<String, String> COMPANY_NAMES = Map.of(
            "V", "Visa",
            "AAPL", "Apple",
            "AMZN", "Amazon",
            "MCD", "McDonald's"
    );

    public CompanyProfile resolve(String ticker) {
        String normalizedTicker = Objects.toString(ticker, "").trim().toUpperCase(Locale.ROOT);
        String companyName = COMPANY_NAMES.getOrDefault(normalizedTicker, normalizedTicker);
        return new CompanyProfile(normalizedTicker, companyName);
    }
}
