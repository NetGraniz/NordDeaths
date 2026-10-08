# NordDeaths 1.1.0

Random English death messages for Paper 26.2 and Folia 26.2. One JAR supports both platforms on Java 25.

## Configuration

Edit the message groups in `plugins/NordDeaths/config.yml`; each group matches a death cause. Run `/norddeaths reload` to load the changes.

NordDeaths handles the death event before NordChat. NordChat's `/toggledeathmsgs`, `/toggledeathmsgshard` and `/ignoredeathmsgs` preferences still apply.

## Permissions

| Permission | Allows | Default |
| --- | --- | --- |
| `norddeaths.admin` | `/norddeaths reload` | Operators |

Players do not need a NordDeaths permission to receive death messages.

## Build and installation

Run `./build.ps1` with Maven 3.9+ and JDK 25. Install `target/NordDeaths-1.1.0.jar` while the server is stopped.

See [BUILDING.md](BUILDING.md) for release requirements and [FOLIA.md](FOLIA.md) for scheduling details.
