package com.jt.summary.config;

import com.jt.domesticamount.service.DomesticAmountService;
import com.jt.domesticamount.service.impl.DomesticAmountServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomesticAmountBeanConfig {

  @Bean
  public DomesticAmountService domesticAmountService() {
    return new DomesticAmountServiceImpl();
  }
}
