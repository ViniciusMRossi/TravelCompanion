# Travel Companion — Canonical Screen Index v2

The approved design contains **19 screens**.

| ID | Screen | Area | Primary purpose |
|---|---|---|---|
| 01 | Quem é você? | Onboarding | Identify the participant on this device |
| 02 | Hoje | Today | Operational command center for the current day |
| 03 | Dia completo | Trip | Full-day timeline and context |
| 04 | Cidade | Explore | Editorial city guide |
| 05 | Atração | Explore | Attraction guide + operational actions |
| 06 | Iniciar passeio | Walk | Pre-flight check before putting phone away |
| 07 | Passeio ativo | Walk | Audio/GPS-first walking experience |
| 08 | Participantes sincronizados | Walk / Sync | Short 3–2–1 transition into shared playback |
| 09 | Ouvir juntos | Audio | Shared audio player and participant states |
| 10 | História pelo caminho | Walk / Story | Location-triggered story surface |
| 11 | Fim do passeio | Walk | Close walk and return user to the day |
| 12 | Gravar memória por voz | Memory | Idle / recording / saved voice-memory flow |
| 13 | Carteira | Wallet | Fast offline access to travel documents |
| 14 | Documento / QR | Wallet | Ticket/document viewer and QR mode |
| 15 | Transporte | Operations | Transport details, critical timing, fallback |
| 16 | Hospedagem | Operations | Stay details, check-in/out, contact and fallback |
| 17 | Emergência | Safety | Immediate emergency actions and contacts |
| 18 | Plano B | Recovery | Calm contingency instructions |
| 19 | Mais | Utilities | Emergency, phrases, apps, group, settings |

## Primary flow

```text
01 Quem é você?
  ↓
02 Hoje
  ↓
05 Atração
  ↓
06 Iniciar passeio
  ↓
07 Passeio ativo
  ↓
08 Participantes sincronizados
  ↓
09 Ouvir juntos
  ↓
10 História pelo caminho
  ↓
11 Fim do passeio
  ↓
12 Gravar memória por voz
  ↓
02 Hoje
```

## State boards

- **S1 Connectivity:** online, offline, stale weather, sync unavailable
- **S2 Participant:** synchronized, connecting, reconnecting, offline
- **S3 Timeline:** completed, active, upcoming, critical
- **S4 Audio:** idle, playing, paused, reconnecting
- **S5 Booking:** reserved, verify, buy, reserved + critical

## Navigation roots

Bottom navigation remains:

1. Hoje
2. Viagem
3. Explorar
4. Carteira
5. Mais

Detail/immersive screens may hide bottom navigation when focus benefits from it, provided Back remains explicit.
