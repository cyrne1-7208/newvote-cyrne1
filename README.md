# NewVote

**[日本語版 README](README_ja.md)**

A Spigot/Paper/Folia plugin that runs in-game player votes with real-time Tab list status visualization.

## Overview
NewVote lets server operators start and stop vote sessions without leaving the game.
Players cast votes with a single command and see their status reflected in the Tab list.
Vote results are shown to all online players as a ranked summary.

### Target Users
- Minecraft server operators running events or minigames that require player votes.
- Players who want to see live voting progress in the Tab list.

## Features
- Real-time Tab list status: `×` before voting, `✓` after voting.
- Ranked vote result display to all players on close.
- Post-vote reveal: who voted for whom shown in the Tab list.
- Tab completion for `/v` based on online players or a fixed target list.
- Fixed vote target list via `config.yml` (overrides online player list).
- Special reveal animation for a specific player (Yanaaaaa-exclusive).
- Debug logging with configurable stack trace output.
- Uses only Bukkit/Spigot/Folia API — no NMS dependency.

## Requirements
- Java 8 or later
- Spigot/Paper/Folia 1.15.2 or later

**Tested versions:**

| Minecraft | Server | Java |
|---|---|---|
| 1.15.2 | Spigot 1.15.2 / Paper 1.15.2 | Java 11 |
| 1.16.5 | Paper 1.16.5 | Java 16 |
| 1.21.11 | Paper 1.21.11 / Folia 1.21.11 | Java 21 |
| 26.2 | Spigot 26.2 / Folia 26.2 | Java 25 |
| 26.3 | Paper 26.3 | Java 25 |

Code-level compatibility: Spigot/Paper/Folia 1.15.2 – 26.3

Note: Sound enum differences across versions are handled by a built-in fallback.

## Status
- Last README update: 2026-10-13
- Supported runtime: Spigot/Paper/Folia 1.15.2 – 26.3 / Java 8+

## Installation
1. Build the plugin jar.
2. Copy the jar to your server's `plugins/` directory.
3. Restart the server.

```bash
mvn -DskipTests package
```

Output: `target/NewVote-v8.77778.2.jar`

## Quick Start
1. Place the jar in `plugins/` and start (or restart) the server.
2. Log in as an OP and run `/vs` to start voting.
3. Players run `/v <target>` to cast votes.
4. Run `/vs` again to close voting and display results.

Expected output:

```text
A ranked vote summary is shown to all online players.
Each entry displays rank, player name, and vote count.
```

## Usage
Start and close voting (OP only):

```text
/vs
```

Cast a vote:

```text
/v <target>
```

Reveal vote destinations (OP only):

```text
/vget
```

### Vote workflow
1. `/vs` — Start vote. All players see `×` in the Tab list.
2. `/v <target>` — Cast a vote. Your Tab list entry changes to `✓`.
3. `/vs` — Close vote. Ranked results are shown to all players.
4. `/vget` — Reveal who voted for whom in the Tab list.
5. `/vs` — Clear Tab list entries.

### Yana feature

```text
/yvote          — Toggle Yana mode on/off (Yanaaaaa only)
/yvote <rank>   — Set rank threshold for the reveal animation (Yanaaaaa only)
```

### Commands
| Command | Permission | Description |
|---|---|---|
| `/v <target>` | `newvote.v` (default: all players) | Cast a vote |
| `/vs` | `newvote.vs` (default: op) | Start/stop voting or clear Tab list |
| `/vget` | `newvote.vs` (default: op) | Reveal vote destinations in Tab list |
| `/yvote [rank]` | — | Yana-exclusive command |

## Configuration
`config.yml`:

```yaml
List: []

debug:
  enabled: false
  logStackTrace: true
```

| Key | Default | Description |
|---|---|---|
| `List` | `[]` | Fixed vote target list. Empty = use online players. |
| `debug.enabled` | `false` | Output state transition logs on each command. |
| `debug.logStackTrace` | `true` | Include stack traces in error logs. |

Fixed target list example:

```yaml
List:
  - "Target1"
  - "Target2"
```

When `List` has entries, voting is restricted to that list regardless of who is online.

## Project Structure
| Path | Purpose |
|---|---|
| `NewVote.java` | Main plugin class, command handling |
| `SchedulerLogic.java` | Spigot/Paper/Folia scheduler handling |
| `ScoreBoardLogic.java` | Tab list name update logic |
| `VoteResultLogic.java` | Vote destination reveal logic |
| `plugin.yml` | Spigot plugin metadata and command definitions |
| `config.yml` | Runtime configuration |

## Testing
There is no automated test suite.
Test with a local Paper server using a small online player group and verify Tab list updates after each step.

## Troubleshooting
- Vote not starting: confirm you are OP and the plugin loaded without errors.
- Target not found: ensure the name exactly matches an online player or a `List` entry.
- Sound errors: the plugin falls back to an alternative `Sound` enum value automatically.
- Tab list not updating: verify the server is running Spigot/Paper/Folia, not vanilla.

## Acknowledgements
- Original concept and production: Yanaaaaa
- Base reference: [TeamKun/VotePlugin](https://github.com/TeamKun/VotePlugin)

## AI Usage
- AI used: GPT-5.3-Codex (README drafting)
- Usage scope: documentation structure, wording
- Human review: commands, configuration keys, and behavior verified against source code

## Contributing
Issues and pull requests are welcome.
Please include:
- Goal of the change
- Summary of implementation
- Reproduction or verification steps

## License
MIT License. See `LICENSE`.

## Support
Open an issue in this repository for bug reports or questions.