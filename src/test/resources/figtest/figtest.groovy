package figtest


dockerfiles {
    redis {
        image 'redis'
        tag '2.8.19'

        containers {
            redis {
                healthcheck(timeout: 20) { container ->
                    container.canOpenTcpSocket(6379)
                }
            }
        }
    }

    web {
        folder 'web'

        containers {
            web {
                linkedTo 'redis:redis'
                mapPort '80:5000'

                healthcheck(timeout: 20, interval: 2) { container ->
                    String response = container.httpGet("/")

                    if (response =~ /Hello... I have been seen \d+ times./) {
                        return response
                    }
                    else {
                        throw new IllegalStateException("Unexpected response: " + response)
                    }
                }
            }
        }
    }

}

afterBoot { args ->
    notifySend("Booted ${args.containers.size()} containers in ${args.duration.standardSeconds} seconds!")
}

beforeReboot { args ->
    notifySend("Will reboot: ${args.containers.join(', ')}!")
}

afterReboot { args ->
    notifySend("Re-booted ${args.containers.join(', ')} in ${args.duration.standardSeconds} seconds!")
}