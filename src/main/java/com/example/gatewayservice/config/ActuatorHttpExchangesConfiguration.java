package com.example.gatewayservice.config;

import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//애플리케이션의 HTTP 요청 및 응답 데이터를 저장하고 관리하는 역할
// InMemoryHttpExchangeRepository 클래스를 사용하여 메모리에 저장
// Spring Actuator에서 HTTP 트래픽을 모니터링하기 위해 사용
@Configuration
public class ActuatorHttpExchangesConfiguration {
    @Bean
    public HttpExchangeRepository httpTraceRepository()
    {
        return new InMemoryHttpExchangeRepository();
    }
}
