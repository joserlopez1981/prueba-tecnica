package com.inditex.priceservice.infrastructure.config;

import com.inditex.priceservice.application.usecase.GetApplicablePriceUseCaseImpl;
import com.inditex.priceservice.domain.port.in.GetApplicablePriceUseCase;
import com.inditex.priceservice.domain.port.out.FindPricePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    public GetApplicablePriceUseCase getApplicablePriceUseCase(FindPricePort findPricePort) {
        return new GetApplicablePriceUseCaseImpl(findPricePort);
    }
}
