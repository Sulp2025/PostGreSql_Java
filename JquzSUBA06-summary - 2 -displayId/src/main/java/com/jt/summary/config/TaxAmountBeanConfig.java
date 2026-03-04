package com.jt.summary.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jt.taxamount.repository.TaxCodeRepository;
import com.jt.taxamount.service.TaxAmountService;
import com.jt.taxamount.service.impl.TaxAmountServiceImpl;

@Configuration
public class TaxAmountBeanConfig {

    @Bean
    public TaxCodeRepository taxCodeRepository(JdbcTemplate jdbcTemplate) {
        return new TaxCodeRepository(jdbcTemplate);
    }

    @Bean
    public TaxAmountService taxAmountService(TaxCodeRepository taxCodeRepository) {
        return new TaxAmountServiceImpl(taxCodeRepository);
    }
}
