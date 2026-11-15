# Chatter

Chatter is a chat plugin for Paper. It splits chat into local and global channels, adds private messages and roleplay commands, and lets every player tune chat to their liking.

## Features

### Local and global chat

- **Two channels.** Local messages reach only players in the same world within a set radius (128 blocks by default). Global messages reach every player who has global chat turned on.
- **Default channel.** Each player chooses where messages without a prefix go: with `/chat local`, `/chat global` or the button in the `/chat` menu. Until a player chooses, the server default from `config.yml` is used.
- **Quick channel switch.** Start a message with `!` to send it to the other channel.
- **"Nobody heard you" notice.** If no one was close enough to hear a local message, the sender is told so.
- **Mention sound.** Players hear a sound when someone writes their name in chat. The name must be a separate word; case does not matter.

### Private messages

- `/w <player> <message>` writes to an online player. `/msg`, `/tell` and `/m` are aliases.
- `/r <message>` replies to the last conversation partner: whoever last wrote to you, or whoever you last wrote to.
- Clicking the other player's name in a private message puts a `/w` command to them into the chat box.
- The recipient hears a sound.
- Players hidden from you, for example vanished ones, count as offline: you cannot write to them, and they are not suggested in tab completion.
- `/socialspy` lets staff with the `chatter.spy` permission see other players' private messages. The setting is kept across relogs and stops working as soon as the permission is removed.

### Roleplay

- `/me <action>` acts out an action: `* Steve opens the door`.
- `/try <action>` acts out an attempt with a random outcome, success or failure with equal odds: `* Steve opens the door [Success]`. Every recipient sees the same outcome.
- `/do <description>` describes the surroundings or the result of an action: `* The door is open (Steve)`.
- These commands send to local chat. `/gme`, `/gtry` and `/gdo` send the same to global chat.
- Roleplay also works right in chat: `*`, `?` and `=` work like `/me`, `/try` and `/do` and send to local chat, while `!*`, `!?` and `!=` send to global chat. The prefix decides the channel, whatever channel is chosen in `/chat`. A prefix with no text after it is sent as a regular message, so a lone `?` stays a question.
- Roleplay follows the same rules as regular chat: the local radius, the global chat setting, the "nobody heard you" notice and the mention sound.

### Message modules

Modules turn text in a message into interactive elements. They work in chat, private messages and roleplay.

- `:loc:` shows your coordinates, colored by dimension. Click them to copy.
- `:item:` shows the item in your main hand, colored by rarity. Hover over it to see its tooltip. With an empty hand the module is removed from the message.

### Personal settings

`/chat` opens a menu where each player sets:

- the default channel;
- global chat: when it is off, global messages are hidden, the player cannot write to global chat, and the default channel switches to local;
- join and quit messages, death messages and advancement messages;
- the mention sound;
- private messages: when they are off, the player can neither receive nor send them;
- the private message sound.

Settings are stored in player data, so they are kept across relogs and server restarts. Players who have not changed a setting get the server default from `config.yml`.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/chat` | Open the chat settings menu | — |
| `/chat <local\|global>` | Set the channel for messages without a prefix | — |
| `/w <player> <message>` | Send a private message (aliases: `/msg`, `/tell`, `/m`) | — |
| `/r <message>` | Reply to the last conversation partner | — |
| `/socialspy` | Toggle watching other players' private messages | `chatter.spy` |
| `/me <action>` | Act out an action in local chat | — |
| `/try <action>` | Act out an attempt with a random outcome in local chat | — |
| `/do <description>` | Describe the surroundings in local chat | — |
| `/gme`, `/gtry`, `/gdo` | The same roleplay commands in global chat | — |
| `/chatter reload` | Reload the configuration | `chatter.reload` |

Only `/chatter reload` can be run from the console; the other commands are for players.

`/w`, `/msg`, `/tell` and `/me` replace the vanilla commands of the same name. The vanilla ones stay available with the `minecraft:` prefix, for example `/minecraft:msg`.

## Permissions

| Permission | Default | Grants |
| --- | --- | --- |
| `chatter.reload` | op | `/chatter reload` |
| `chatter.spy` | op | `/socialspy` and seeing other players' private messages |

## Configuration

`config.yml` is created in `plugins/chatter/` on the first start. Every option is described by a comment in the file. Formats and messages use [MiniMessage](https://docs.papermc.io/adventure/minimessage/format/).

| Section | What it sets |
| --- | --- |
| `default-channel`, `default-preferences` | Defaults for players who have not changed their settings |
| `local`, `global` | The local chat radius, the `!` prefix and the chat formats |
| `mention-sound` | The mention sound |
| `modules` | The `:loc:` and `:item:` modules |
| `private-messages` | Private message formats, the sound and messages |
| `roleplay` | Roleplay prefixes, formats and `/try` outcomes |
| `messages` | General plugin messages |
| `menu` | Texts of the `/chat` menu |

`/chatter reload` applies changes without a restart. If a key is missing from your `config.yml`, for example after an update that added new options, the bundled default is used, so the file does not need to be edited after updating. Invalid values, such as a negative radius or an unknown sound, are replaced with defaults and reported in the console. A roleplay prefix can be turned off by setting it to an empty string; a prefix that clashes with the `!` prefix or with another roleplay prefix is turned off with a warning.

## Installation

1. Make sure the server runs Paper 26.3 on Java 26.
2. Put `chatter-<version>-all.jar` into the `plugins` folder.
3. Start the server. Edit `plugins/chatter/config.yml` if needed and run `/chatter reload`.

## Building

    ./gradlew shadowJar

The plugin jar is written to `build/libs/chatter-<version>-all.jar`. Use the `-all` jar: it bundles the Kotlin runtime the plugin needs.

`./gradlew runServer` starts a local Paper 26.3 test server with the plugin installed.

Every push and pull request to `stable` is built on GitHub Actions, and the jar is attached to the run as the `chatter` artifact for 14 days.
