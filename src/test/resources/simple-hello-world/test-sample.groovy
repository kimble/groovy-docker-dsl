

dockerfiles {
    helloWorldPhp {
        folder 'php-hello-world'
        repository 'developerb'
        tag 'integration-test'

        containers {
            phpHelloWorld {
                healthcheck { container ->
                    "http://${container.ipAddress()}".toURL().getText().trim() == "Hello world"
                }
            }
        }
    }
}