package com.smartstadium.ticketing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the ticketing classes into Spring. The classes themselves are plain Java, so they are easy to test.
 * Set {@code smartstadium.tickets.secret} in application.properties for a real deployment.
 */
@Configuration
public class TicketConfig {
    private static final Clock CLOCK = Clock.systemUTC();

    @Bean
    public TicketSigner ticketSigner(
            @Value("${smartstadium.tickets.secret:dev-only-ticket-secret-change-me}") String secret) {
        return new TicketSigner(secret);
    }

    @Bean
    public TicketRegistry ticketRegistry(TicketSigner signer) {
        return new TicketRegistry(signer, CLOCK);
    }

    @Bean
    public TicketService ticketService(TicketSigner signer, TicketRegistry registry) {
        return new TicketService(new TicketGenerator(signer, CLOCK), registry);
    }
}
