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

To run the Paper regression suite, including a real server restart, first run
`./checks/run.sh`, then provide a trusted Paper 26.3 server JAR:

```sh
PAPER_JAR=/path/to/paper-26.3.jar ./checks/run-paper.sh
```

The runner creates a disposable server inside ignored `.verification/`, binds only
to localhost, keeps online authentication enabled, and stops after the checks.
It requires `timeout` and starts the test server with the Minecraft EULA accepted.
Do not install the regression probe on a live server.

Normal profile access uses `playerprofiles.profile` for your own profile and
`playerprofiles.profile.others` for another player's profile, including right-click
interaction. Normal commands and menu navigation respect locks, visibility,
disabled worlds/regions and combat restrictions. Owners can still open their own
locked profile. Admin `openprofile` and `opengui` commands deliberately retain
their forced-open bypass, including navigation from those forced menus; item-level
permissions still apply. Lock changes save immediately using a complete file replacement.

See [ASSESSMENT.md](ASSESSMENT.md) for compatibility verification and the issue resolutions.

## License

You can do whatever you want with the source code, just don't redistribute it.
