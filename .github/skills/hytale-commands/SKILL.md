# Hytale Command System Skill

This skill documents how to create commands in Hytale plugins, including permission configuration.

## Command Base Classes

| Class | Use Case |
|-------|----------|
| `AbstractCommand` | Base for all commands |
| `AbstractPlayerCommand` | Commands that require a player sender |
| `AbstractAsyncCommand` | Commands that run async logic |
| `AbstractCommandCollection` | Parent command that groups subcommands |
| `CommandBase` | Simple command wrapper (Hyforged custom) |

## Creating a Basic Command

```java
public class MyCommand extends AbstractPlayerCommand {
    public MyCommand() {
        super("mycommand", "my.plugin.commands.mycommand.desc");
    }
    
    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        // Command logic here
        playerRef.sendMessage(Message.raw("Hello!"));
    }
}
```

## Permission System

### How Permissions Work

1. **Auto-generated permissions**: By default, commands auto-generate permission nodes based on the plugin's base permission + command hierarchy
2. **Permission check flow**: When executing, `hasPermission()` checks:
   - The command's own permission
   - The parent command's permission (recursively up the chain)
3. **Permission groups**: Associate commands with game modes (Creative, Adventure, etc.)

### Making a Command Require No Permission

To make a command usable by all players without any permission:

**Option 1: Override `canGeneratePermission()` (RECOMMENDED)**
```java
public class MyPublicCommand extends AbstractPlayerCommand {
    public MyPublicCommand() {
        super("mycommand", "desc");
    }
    
    @Override
    protected boolean canGeneratePermission() {
        return false; // Prevents permission from being generated
    }
    
    // ... execute method
}
```

**Option 2: Using `setPermissionGroup(null)`**
```java
public MyCommand() {
    super("mycommand", "desc");
    this.setPermissionGroup(null); // Sets permission groups to null
}
```

> **IMPORTANT**: `setPermissionGroup(null)` alone is NOT sufficient if the command still has an auto-generated permission string. You MUST override `canGeneratePermission()` to return `false`.

### For Subcommands

When a command is a subcommand (e.g., `/hyforged hub`), BOTH the parent AND the subcommand must allow no-permission execution:

```java
// Parent command collection
public class ParentCommand extends AbstractCommandCollection {
    public ParentCommand() {
        super("parent", "desc");
        this.addSubCommand(new ChildCommand());
    }
    
    @Override
    protected boolean canGeneratePermission() {
        return false; // Parent must also skip permission
    }
}

// Child command
public class ChildCommand extends AbstractAsyncCommand {
    public ChildCommand() {
        super("child", "desc");
    }
    
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
    
    // ... executeAsync method
}
```

### Mixed Permission Model

If you want SOME subcommands public and SOME requiring permissions:

```java
public class ParentCommand extends AbstractCommandCollection {
    public ParentCommand() {
        super("parent", "desc");
        this.addSubCommand(new PublicCommand());  // No permission needed
        this.addSubCommand(new AdminCommand());   // Requires permission
    }
    
    @Override
    protected boolean canGeneratePermission() {
        return false; // Parent allows all, children decide individually
    }
}

public class PublicCommand extends AbstractPlayerCommand {
    @Override
    protected boolean canGeneratePermission() {
        return false; // Public
    }
}

public class AdminCommand extends AbstractPlayerCommand {
    public AdminCommand() {
        super("admin", "desc");
        this.requirePermission("myplugin.admin.command"); // Explicit permission
    }
}
```

## Registration

Register commands in your plugin's `setup()` method:

```java
@Override
public void setup() {
    getCommandRegistry().registerCommand(new MyCommand());
}
```

## Sending Messages

```java
// To player via PlayerRef
playerRef.sendMessage(Message.raw("Hello!"));

// With color (use hex string, NOT int)
playerRef.sendMessage(Message.raw("Success!").color("#55FF55"));

// Chained messages using insert()
Message msg = Message.raw("Prefix: ").color("#AAAAAA")
    .insert(Message.raw("Value").color("#FFFFFF"));
playerRef.sendMessage(msg);
```

## Command Context

```java
context.sender();           // CommandSender (could be console or player)
context.sendMessage(msg);   // Send message to sender
context.getArg(myArg);      // Get argument value
```

## Related Files

- `com.hypixel.hytale.server.core.command.system.AbstractCommand`
- `com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand`
- `com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand`
- `com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection`
