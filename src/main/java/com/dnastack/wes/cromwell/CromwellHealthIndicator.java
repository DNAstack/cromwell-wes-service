package com.dnastack.wes.cromwell;

import com.dnastack.wes.agent.Check;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CromwellHealthIndicator implements HealthIndicator {

    private final CromwellClient cromwellClient;

    public CromwellHealthIndicator(CromwellClient cromwellClient) {
        this.cromwellClient = cromwellClient;
    }

    @Override
    public Health health() {
        try {
            cromwellClient.getStatus();
            return Health.up().build();
        } catch (FeignException e){
            log.error("Cromwell instance unhealthy",e);
            return Health.down(e).withDetail("message",e.contentUTF8()).build();
        } catch (Exception e){
            return Health.down(e).build();
        }
    }

    public Check.CheckOutcome check() {
        try {
            cromwellClient.getStatus();
            return Check.CheckOutcome.SUCCESS;
        } catch (Exception e){
            log.error("Cromwell instance unhealthy",e);
            return Check.CheckOutcome.FAILURE;
        }
    }
}
