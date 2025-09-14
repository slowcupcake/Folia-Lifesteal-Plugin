# LifeSteal Plugin (Folia-Compatible)

A simple Minecraft plugin that allows players to **steal health** from other players on PvP kills. Fully compatible with **Folia**.

## Features

- Life steal on killing blows.
- Configurable fraction of victim's max health to steal.
- Optional messages to notify players when health is gained.
- Folia-compatible (`folia-supported: true` in `plugin.yml`).

## Installation

1. Build the plugin with Maven:  
```bash
mvn clean package
```
2. Place the generated `.jar` in your server’s `plugins/` folder.
3. Start the server — a default `config.yml` will be generated.

## Configuration (`config.yml`)

```yaml
# Fraction of victim's max health transferred to the killer on kill
steal-percent: 0.25

# Show a message to the killer when health is stolen
announce-on-steal: true
msg-gained: "You stole {amount} hearts!"
```

- `steal-percent`: 0.25 → 25% of the victim's max health.
- `announce-on-steal`: true/false → toggle messages.
- `{amount}` is replaced with the number of hearts gained.

## Usage

- Engage in PvP. When a player kills another player, they gain a portion of the victim's max health as configured.

## Development

- Java 17+ recommended.
- Built with Maven.
- Follows Folia’s threading model for compatibility.

## License

MIT License
