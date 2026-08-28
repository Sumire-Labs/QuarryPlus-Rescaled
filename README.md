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

The remapped distribution jar is written to `build/libs` without a classifier.
Use `runClient` or `runServer` for development runs and `genSources` when IDE
Minecraft sources are needed.

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

