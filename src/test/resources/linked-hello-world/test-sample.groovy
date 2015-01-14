package linked

dockerfiles {
    calculator {
        folder 'calculator'
        repository 'developerb'
        tag 'integration-test'

        containers {
            calculator {
                healthcheck { container ->
                    def result = "http://${container.ipAddress()}?a=1&b=2".toURL().getText().trim()

                    if (result.contains("3")) {
                        return result;
                    }
                    else {
                        throw new IllegalStateException("Didn't expect: " + result)
                    }
                }
            }
        }
    }

    client {
        folder 'client'
        repository 'developerb'
        tag 'integration-test'

        containers {
            client {
                linkedTo 'calculator:calculator'

                healthcheck { container ->
                    def result = "http://${container.ipAddress()}".toURL().getText().trim()

                    if (result.contains("2 + 3 = 5")) {
                        return result;
                    }
                    else {
                        throw new IllegalStateException("Didn't expect: " + result)
                    }
                }
            }
        }
    }
}