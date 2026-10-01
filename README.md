# discordbot
[Spring Boot](https://github.com/spring-projects/spring-boot) Bot that uses the [Discord4j](https://github.com/Discord4J/Discord4J) API

Current Features:
- create AI Generated images for text input, using DALL-E/OpenAI API
- Posting Nasa Picture of the day
- Playing YouTube/random local sounds
- Random memes
- embedded Help command with command descriptions

## Build and deployment

Use JDK 26 and Maven 3.6.3 or newer. Run `mvn clean verify` to compile,
test, and package the application. Docker is required for
`mvn spring-boot:build-image-no-fork` after packaging. The default Paketo builder
uses a Java 26 runtime, inferred from `java.version`. Kotlin 2.4.20 uses the same
JVM target through the Spring Boot parent configuration.

The Azure DevOps pipeline builds commits on `master`, runs verification, and
pushes the image to `qdsoftware.azurecr.io/discordbot` with the build ID,
application version, and `latest` tags. It deploys the build ID tag to container
`discordbot` in Deployment `qdsoftware/discordbot`, then waits up to 180 seconds
for the rollout. Other branches can be built manually but do not push or deploy.

Authorize this pipeline to use the `qdsoftware-discordbot` Kubernetes service
connection and the existing `qdsoftware` Docker registry service connection.
The Kubernetes connection uses service account `qdsoftware/azure-deployment`,
which can update and monitor only the `discordbot` Deployment. Registry pulls
use the existing `qdsoftware-azure` image pull Secret in the cluster.

The current Kubernetes Deployment uses rolling updates, which can briefly run
two bot instances. Rollout success checks Kubernetes availability; the current
Deployment has no readiness probe to verify that the bot has connected to Discord.
