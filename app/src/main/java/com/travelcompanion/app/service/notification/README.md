# Notifications

Four channels, all local. Nothing here reads the network, so a notification
behaves the same with the radios off — which the brief asks for literally, and
which is the state this trip is mostly in.

| Channel | Id | Importance | Posted by | Status |
| --- | --- | --- | --- | --- |
| Prazos da viagem | `operational` | HIGH | `CriticalAlertReceiver` | live |
| Histórias pelo caminho | `stories` | DEFAULT | Walk Mode, and passive discovery | live since Phase 3 |
| Passeio | `walk` | LOW | the foreground service | live since Phase 3 |
| Memórias | `memory` | LOW | — | **created, posts nothing** |

## Operational — where the deadlines come from

The decision is a pure function, `domain/alerts/criticalAlerts`, and the
`AlarmManager` side is handed a list that was already decided. That is where
the risk lives: an hour out is invisible on every screen and only wrong at the
gate.

- **the text is the content's.** Title is `criticalItem.title`, body is
  `criticalItem.instruction`. Nothing is composed. An item with no instruction
  is not announced — silence beats invented text;
- **the instant is the day's date plus `actionByTime`, read in the zone the
  package wrote it in**: the timeline item's, when it declares one, and the
  day's when it does not. Only then does it become an instant;
- **one alert per critical item**, at its earliest occurrence. An
  accommodation's item is pulled into every day of the stay, and Kotor's
  "check-in fecha às 21:00" is one deadline and not three;
- **nothing in the past** is scheduled;
- **a tap opens the thing's own screen** — 15 for a transport, 16 for a stay —
  through `Routes`, which is the one definition of those strings.

Rescheduled after boot, after an app update, when the exact-alarm permission
changes, and whenever the content or the traveller does.

## Stories — the same notification, two ways in

One builder and one permission guard, in `WalkNotifications` and
`AndroidWalkPresence`. The two paths differ in one thing, and it is the thing
that has to differ: **where a tap lands**. During a walk it returns to the walk
the traveller already has, with screen 10 over it. Outside a walk there is no
screen for that sheet to rise over, so the tap asks for screen 04 — through
`OperationalNotifications.openAt`, which is already the one definition of "a
notification that opens a particular screen" (D105 (a)).

## Memory

The channel exists so the traveller can switch it off before it ever speaks.
What the evening prompt would say, at what hour, and whether it is every night
or only nights with a walk, is copy no approved sheet draws — it is in the
design confirmation stack rather than invented here.
