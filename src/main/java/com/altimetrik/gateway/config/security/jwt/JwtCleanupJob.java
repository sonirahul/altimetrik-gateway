package com.altimetrik.gateway.config.security.jwt;

import com.altimetrik.gateway.config.Constants;
import com.altimetrik.gateway.repository.AuthenticationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.invoke.MethodHandles;
import java.util.Date;

@Configuration
@EnableScheduling
public class JwtCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private AuthenticationRepository authenticationRepository;

    public JwtCleanupJob(AuthenticationRepository authenticationRepository) {
        this.authenticationRepository = authenticationRepository;
    }

    /**
     * This is a clean up job
     * this deletes all the tokens from the DB which have expired.
     * runs every 10 mins
     */
    @Scheduled(fixedRate = 1000 * 60 * Constants.TEN)
    public void scheduleFixedRateTask() {
        log.debug("Deleting the expired tokens");
        authenticationRepository.deleteAllByValidityBefore(new Date());
    }
}
