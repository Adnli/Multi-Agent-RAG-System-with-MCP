package com.example.finnews.service;

import com.example.finnews.model.CompanyProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyProfileResolverTest {

    private final CompanyProfileResolver resolver = new CompanyProfileResolver();

    @Test
    void resolvesKnownFrontendTickersToCompanyNames() {
        assertThat(resolver.resolve("V")).isEqualTo(new CompanyProfile("V", "Visa"));
        assertThat(resolver.resolve("AAPL")).isEqualTo(new CompanyProfile("AAPL", "Apple"));
        assertThat(resolver.resolve("AMZN")).isEqualTo(new CompanyProfile("AMZN", "Amazon"));
        assertThat(resolver.resolve("MCD")).isEqualTo(new CompanyProfile("MCD", "McDonald's"));
    }

    @Test
    void normalizesTickerAndFallsBackToTickerForUnknownSymbol() {
        CompanyProfile profile = resolver.resolve(" nvda ");

        assertThat(profile.ticker()).isEqualTo("NVDA");
        assertThat(profile.companyName()).isEqualTo("NVDA");
        assertThat(profile.searchPrefix()).isEqualTo("NVDA NVDA");
    }
}
