# NordDeaths 1.1.0

One release JAR for Paper 26.2 and Folia 26.2: [compatibility notes](FOLIA.md).

> Release build and installation requirements: see [BUILDING.md](BUILDING.md).
> Older local paths below describe historical test fixtures, not the release build.

Small random English death-message plugin for Nord Fjell. Messages live in `config.yml` and are grouped by cause.

NordDeaths runs before NordChat's death-message listener. NordChat therefore continues to honor `/toggledeathmsgs`, `/toggledeathmsgshard` and `/ignoredeathmsgs` for the customized messages.

Build with `./build.ps1`. Install `target/NordDeaths-1.1.0.jar` only while the server is stopped.
