package org.prebid.pg.delstats.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.prebid.pg.delstats.model.dto.Shutdown;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints to manage a graceful shutdown of the application.
 */
@Slf4j
@RestController
@RequestMapping("${services.admin-base-url}")
@Api(tags = "admin")
public class ShutdownController {

    private Shutdown shutdown;

    public ShutdownController(Shutdown shutdown) {
        this.shutdown = shutdown;
    }

    /**
     * Set the system into a 'ready for shutdown' mode causing most endpoints to return immediately indicating the
     * application is shutting down.
     */
    @PostMapping(value = "/v1/prep-for-shutdown")
    @ResponseStatus(HttpStatus.OK)
    public void set() {
        this.setShutdown(Boolean.TRUE);
    }

    /**
     * Abort the 'ready for shutdown' mode returning the application endpoints and scheduled events to normal operation.
     */
    @PostMapping(value = "/v1/cease-shutdown")
    @ResponseStatus(HttpStatus.OK)
    public void unset() {
        this.setShutdown(Boolean.FALSE);
    }

    private void setShutdown(Boolean state) {
        log.info("old shutdown=" + shutdown);
        shutdown.setInitiating(state);
        log.info("new shutdown=" + shutdown);
    }
}
