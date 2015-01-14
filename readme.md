Groovy Docker DSL
=================

Think [Fig](http://www.fig.sh/) with a Groovy DSL letting you implement hooks, health checks and monitors
folders containing Dockerfiles for changes and automatically re-build and reboot containers as needed.

Build'n'run
-----------
The project is built using Gradle and ships with a wrapper.

    ./gradlew shadowJar
    java -jar build/libs/docker-manager-all.jar src/test/resources/simple-hello-world/test-sample.groovy


Still to do..
-------------
1. Support more Docker features
    1. Volumes
    2. Resource constraints (cpu / memory..)
    3. Networking options