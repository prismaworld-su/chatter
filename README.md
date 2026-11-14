# Chatter

Chatter splits server chat into local and global channels and lets every player tune chat to their liking.

## Features

- **Local and global chat.** Local messages reach only players in the same world within a set radius (128 blocks by default). Global messages reach the whole server.
- **Quick channel switch.** Start a message with `!` to send it to the other channel. `/chat local` and `/chat global` set where messages without a prefix go.
- **Chat settings menu.** `/chat` opens a menu where players choose their channel and turn global chat, join and quit messages, death messages, advancement messages and the mention sound on or off.
- **Mention sound.** Players hear a sound when someone writes their name in chat.
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
| `/chatter reload` | Reload the configuration | `chatter.reload` (op) |

## Requirements

- Paper 26.3
- Java 26

## Building

    ./gradlew shadowJar

The plugin jar is written to `build/libs/`.
