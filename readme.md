Groovy Docker DSL
=================

Think [Fig](http://www.fig.sh/) with a Groovy DSL letting you implement hooks, health checks and monitors
folders containing Dockerfiles for changes and automatically re-build and reboot containers as needed.

**Motivation:** Fig is great, but I needed something that would allow me to do things like waiting until the
service running inside a container is actually operational before booting the next and rebuild and reboot containers
as they change.

**Warning:** I've just started this project, I'm sure it has a lot of bugs and rough edges.


Build'n'run
-----------
The project is built using Gradle and ships with a wrapper.

    ./gradlew shadowJar
    java -jar build/libs/groovy-docker-dsl-all.jar src/test/resources/figtest/figtest.groovy


Still to do..
-------------
1. Support more Docker features like
    1. Volumes
    2. Resource constraints (cpu / memory..)
    3. Networking options
2. Experiment with doing some operations in parallel (Docker might not like this..)