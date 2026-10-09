# QuarryPlus(Additional Enchanted Miner) Rescaled 

QuarryPlus Rescaled adds advanced quarries and related machines. This branch is
for [CleanroomLoader](https://github.com/CleanroomMC/Cleanroom) only and is not
compatible with legacy Forge installations.

The project uses Java 25, Scala 3, Scalar, Unimined, and MCP stable_39. Scalar
is a required runtime mod and is declared as a mandatory dependency.

## Building

Install a Java 25 JDK, then run:

```shell
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

If Java on Windows reports `Unable to establish loopback connection`, set
`-Djdk.net.unixdomain.tmpdir` to an existing local directory for the Gradle JVMs
(for example through `JAVA_TOOL_OPTIONS`). Verification on this machine used
the ignored `.gradle/sockets` directory without changing global Java settings.

The remapped distribution jar is written to `build/libs` without a classifier.
Use `runClient` or `runServer` for development runs and `genSources` when IDE
Minecraft sources are needed.

Versions are derived from Git by
[CleanroomVersioning](https://github.com/CleanroomMC/Versioning) 3.3.0.
Run `./gradlew -q printVersion` to inspect the version and
`./gradlew -q printReleaseStage` to inspect its upload stage. The release branch
is `1.12`; CI checkouts must include full history and tags.
Publishing workflows use `writeReleaseMetadata` to read the same values from
`build/release-metadata.properties`, without including plugin startup logging.

Release numbers and stages come from tags such as `13.0.0-alpha`, `13.0.1-beta`,
or `13.0.2-release` (an optional `v` prefix is accepted). The stage suffix does
not become part of the numeric artifact version. Use a new numeric version for
each release, including transitions between stages. Development builds add a
commit counter, and local changes add `.local.dirty`.

The old `mod_version = 13.0.0-dev` setting is no longer used. Until a Rescaled
release tag is created, the inherited valid `v1.0.0` tag produces `1.0.1-dev.N`
builds. Legacy tags ending in `-1.12.2` are ignored by Versioning. Start the
13.x line by tagging the intended release commit, for example `13.0.0-alpha`.

The manual CurseForge/Modrinth workflow takes an existing release tag as input
and builds that exact tag. Its project IDs still need to be configured before
publishing.

Metadata constants and resources use
[TokenEnvoy](https://github.com/CleanroomMC/TokenEnvoy) 3.0.0 with `@{name}`
markers in normal source directories. Compiled constants, annotations, and
processed resources receive the computed version. Source JARs retain the
original markers because TokenEnvoy does not rewrite source files.

## Cleanroom development notes

This project uses a custom [Unimined fork](https://github.com/kappa-maintainer/Unimined)
([original](https://github.com/unimined/Unimined)). Report impossible field
names or impossible Scala compiler errors to this project or the Unimined fork.

If you are using IntelliJ, **DO NOT** use the `Minecraft Client` configuration
with a blue icon. Use the `2. Run Client` Gradle task.

Mod dependencies belong in `gradle/scripts/dependencies.gradle`. Use
`modImplementation` or `modRuntimeOnly` for mods that must be remapped, and
`modLibrary` for libraries or mods that must remain unmapped. The `contain` and
`shadow` configurations are available for non-mod dependencies as described in
that file.

Access transformers must use MCP names; Unimined remaps them to SRG names in
the artifact. Access transformers are currently disabled for QuarryPlus.

Run `genSources` to generate commented vanilla sources. If it does not produce
a `-sources.jar` on the first run, run the task again.

## Binary compatibility

Add-ons compiled against the former Scala 2 build must be rebuilt against this
Scala 3/Cleanroom branch because Scala types exposed by the mod have a new ABI.

## Supported integrations

- BuildCraft
- CoFH Core and Redstone Flux
- IndustrialCraft 2
- JEI
- CraftTweaker
- Mekanism
- Forge Energy, ItemHandler, and FluidHandler APIs

## LICENSE

Licensed under the LGPL-3.0

## Credits
