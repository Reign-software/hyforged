# Hytale plugin
This is a Hytale plugin project. Hytale plugins are used to extend the functionality of the Hytale server. Plugins can add new features, modify existing behavior, or integrate with other systems. Plugins are typically written in Java and packaged as JAR files that can be loaded by the Hytale server. The plugins are sent to the client at runtime so they are only needed on the server.

- The `.memory_bank` directory contains important context such as ADRs, requirements, and design decisions.
- Hytale uses an Entity Component System (ECS) architecture
- Before implementing new features, review the ECS patterns and existing components in the Hytale server code.
- Use `.doc` for looking up domain knowledge.
- The source code for the Hytale server can be found in the `lib/hytale-server/src/main/java/com/hypixel` directory.
- The games JSON that makes up all items, blocks, and other in-game assets can be found in the `lib/Server` directory. you can use this to look up item IDs, block IDs, and other in-game assets. Do not modify these files directly, they are for reference. We have our own data under `src/main/resources/Server/Hyforged`.
- The `Modding_Doc` folder can be used to store documentation related to modding Hyforged as well as references.

## Project Structure
```text
your-plugin-name/
|-- src/
|   `-- main/
|       |-- java/
|       |   `-- com/
|       |       `-- yourname/
|       |           `-- yourplugin/
|       |               `-- YourPlugin.java
|       `-- resources/
|           |-- manifest.json
|           |-- Common/          # Assets (models, textures)
|           `-- Server/          # Server-side data
|-- build.gradle
|-- settings.gradle
|-- gradle.properties
|-- README.md
`-- run/                         # Generated when you run the server

```