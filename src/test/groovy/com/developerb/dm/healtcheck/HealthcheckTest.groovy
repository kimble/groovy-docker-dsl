package com.developerb.dm.healtcheck

import org.junit.Test
import org.slf4j.Logger

class HealthcheckTest {

    Set<String> loggedToInfo = [] as Set


    Logger testLogger = [
            info : { String msg ->
                loggedToInfo << msg
            }
    ] as Logger


    @Test
    void returningStringIndicatesHealthyContainer() {
        def healthcheck = new Healthcheck(testLogger, {
              return "all is well"
        })

        def result = healthcheck.probe(null)
        assert result == Result.healthy("all is well")
    }

    @Test
    void returningTrueIndicatesHealthyContainer() {
        def healthcheck = new Healthcheck(testLogger, {
              return true
        })

        def result = healthcheck.probe(null)
        assert result == Result.healthy()
    }

    @Test
    void returningFalseIndicatesUnHealthyContainer() {
        def healthcheck = new Healthcheck(testLogger, {
              return false
        })

        def result = healthcheck.probe(null)
        assert result == Result.unhealthy()
    }

    @Test
    void returningNullIndicatesUnHealthyContainer() {
        def healthcheck = new Healthcheck(testLogger, {
              return null
        })

        def result = healthcheck.probe(null)
        assert result == Result.unhealthy("Health check returned null")
    }

    @Test
    void loggingIsAvailableToHealthChecks() {
        def healthcheck = new Healthcheck(testLogger, {
            log.info("Halla")
            return true
        })

        def result = healthcheck.probe(null)

        assert result == Result.healthy()
        assert loggedToInfo == [ "Halla" ] as Set
    }

}
