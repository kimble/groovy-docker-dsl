package figtest


dockerfiles {
    redis {
        image 'redis'
        tag '2.8.19'

        containers {
            redis {
                healthcheck { container ->
                    def socket = null
                    def ipAddress = container.ipAddress()

                    try {
                        socket = new Socket(ipAddress, 6379)
                        return 'Created socket for ' + ipAddress + ':6379'
                    }
                    finally {
                        socket?.close()
                    }
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

                healthcheck { container ->
                    def response = "http://${container.ipAddress()}".toURL().getText().trim()
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
    sendNotification("Booted ${args.containers.size()} containers in ${args.duration.standardSeconds} seconds!")
}

beforeReboot { args ->
    sendNotification("Will reboot: ${args.containers.join(', ')}!")
}

afterReboot { args ->
    sendNotification("Re-booted ${args.containers.join(', ')} in ${args.duration.standardSeconds} seconds!")
}



void sendNotification(String message) {
    try {
        [ 'notify-send', message].execute()
    }
    catch (Exception ex) {
        log.warn("Failed to display notification {}", ex.message)
    }
}
