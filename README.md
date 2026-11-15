# Chatter

Chatter splits server chat into local and global channels and lets every player tune chat to their liking.

## Features

- **Local and global chat.** Local messages reach only players in the same world within a set radius (128 blocks by default). Global messages reach the whole server.
- **Quick channel switch.** Start a message with `!` to send it to the other channel. `/chat local` and `/chat global` set where messages without a prefix go.
- **Chat settings menu.** `/chat` opens a menu where players choose their channel and turn global chat, join and quit messages, death messages, advancement messages, the mention sound, private messages and the private message sound on or off.
- **Mention sound.** Players hear a sound when someone writes their name in chat.
- **Private messages.** `/w` writes to an online player and `/r` replies to the last conversation partner. Clicking the other player's name starts a reply. Staff can watch private messages with `/socialspy`.
- **Roleplay.** `/me`, `/try` and `/do` act out what a character does in local chat; `/gme`, `/gtry` and `/gdo` do the same in global chat. `/try` adds a random outcome: success or failure. Prefixes work right in chat: `*`, `?` and `=` send roleplay to local chat, and `!*`, `!?` and `!=` send it to global chat.
- **Message modules.**
  - `:loc:` shows your coordinates, colored by dimension. Click them to copy.
  - `:item:` shows the item in your hand, colored by rarity. Hover over it to see its tooltip.
- **Saved preferences.** Each player's choices are kept across relogs and server restarts.
- **Fully configurable.** Formats, messages, colors, sounds and menu texts are set in `config.yml` using MiniMessage. `/chatter reload` applies changes without a restart.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/chat` | Open the chat settings menu | — |
| `/chat <local\|global>` | Set the channel for messages without a prefix | — |
| `/w <player> <message>` | Send a private message (aliases: `/msg`, `/tell`, `/m`) | — |
| `/r <message>` | Reply to the last conversation partner | — |
| `/socialspy` | Toggle watching other players' private messages | `chatter.spy` (op) |
| `/me <action>` | Act out an action in local chat (`* Steve opens the door`) | — |
| `/try <action>` | Attempt an action with a random outcome in local chat | — |
| `/do <description>` | Describe the surroundings in local chat (`* The door is open (Steve)`) | — |
| `/gme`, `/gtry`, `/gdo` | The same roleplay commands in global chat | — |
| `/chatter reload` | Reload the configuration | `chatter.reload` (op) |

## Requirements

- Paper 26.3
- Java 26

## Building

    ./gradlew shadowJar

The plugin jar is written to `build/libs/`.
