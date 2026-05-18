package com.example.finnews.model;

public record CompanyProfile(
        String ticker,
        String companyName
) {
    public String searchPrefix() {
        return ticker + " " + companyName;
    }

    public String displayName() {
        return companyName + " (" + ticker + ")";
    }
}
