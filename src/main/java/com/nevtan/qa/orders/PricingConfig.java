package com.nevtan.qa.orders;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * This class does not compile, on purpose. It is the build-failure half of the
 * QA matrix: deploy this branch and the build must fail before any container
 * starts, with both errors visible in the build log.
 */
@Configuration
public class PricingConfig {

    // Error 1: incompatible types, String cannot be converted to BigDecimal.
    @Bean
    public BigDecimal defaultTaxRate() {
        return "0.18";
    }

    // Error 2: cannot find symbol, there is no such method on LogScenarios.
    @Bean
    public String pricingBanner(LogScenarios scenarios) {
        return scenarios.describePricingRules();
    }
}
