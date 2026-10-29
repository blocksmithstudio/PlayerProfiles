# PlayerProfiles

A configurable player profile menu with custom GUIs, equipment previews,
PlaceholderAPI support, profile locking, and optional combat and region checks.
For support, add **mdaffa** on Discord.

Build with **JDK 25** and Maven 3.9 or newer:

```sh
mvn clean package
```

The distributable is `target/PlayerProfiles-8.0.4.jar`. MDLib **2.4.7** is
bundled and relocated inside the plugin; it does not need a separate installation.
This MDLib release requires **Java 25 at runtime**, including on older servers.
The default build retains the existing Spigot API baseline. To compile against
Paper 26.3 explicitly:

```sh
mvn -Ppaper-26.3 clean package
```

Run the focused profile and GUI-loading checks with JDK 25 and Maven on `PATH`:

```sh
./checks/run.sh
# Or: ./checks/run.sh -Ppaper-26.3
```

See [ASSESSMENT.md](ASSESSMENT.md) for compatibility verification and existing issues.

## License

You can do whatever you want with the source code, just don't redistribute it.
