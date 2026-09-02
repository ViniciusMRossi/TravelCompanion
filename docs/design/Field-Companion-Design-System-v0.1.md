# Travel Companion — Design System v0.1

Status: **Foundation / pre-UI**
Platform target: **Android**
Reference trip: **Mochilão pelos Bálcãs 2026**

---

# 1. Purpose

This design system defines the visual, interaction, and component foundations of **Travel Companion** before individual screens are designed.

It must support two very different needs without making the product feel fragmented:

1. **Operational travel**
   - time
   - transport
   - check-in/check-out
   - tickets
   - warnings
   - weather
   - emergency
   - Plan B

2. **Travel experience**
   - cities
   - attractions
   - stories
   - photography
   - audio guides
   - walking tours
   - voice memories

The resulting visual language should feel like:

> **A personal field guide backed by a highly reliable travel instrument.**

---

# 2. Design principles

## 2.1 Context before navigation

The interface prioritizes what matters **now** over exposing the entire information architecture.

## 2.2 Calm by default, loud when necessary

Most of the interface is quiet and editorial.

Critical information may deliberately break the visual rhythm.

Examples:

- boarding deadline;
- check-in closing;
- missing document;
- change of terminal;
- emergency.

## 2.3 Editorial for discovery, utilitarian for action

Long-form content may use expressive typography, large photography, captions and breathing room.

Operational content uses:

- sans-serif typography;
- compact layouts;
- explicit labels;
- predictable controls.

## 2.4 Thumb-friendly and outdoor-friendly

Assume the user may be:

- walking;
- holding the phone with one hand;
- under strong sunlight;
- carrying a backpack;
- wearing headphones;
- tired;
- under time pressure.

## 2.5 Offline confidence

Offline is not an error state.

The interface must distinguish between:

- content available locally;
- live data unavailable;
- synchronization unavailable.

## 2.6 Personality without decoration overload

The app may feel personal and adventurous, but should not look like a scrapbook or generic travel agency app.

---

# 3. Visual direction

## Name

**Field Companion**

A hybrid of:

- field guide;
- travel journal;
- editorial photography;
- modern Android utility interface.

## Visual characteristics

- warm paper-like backgrounds;
- dark slate typography;
- restrained teal;
- antique gold details;
- moss green confirmation states;
- oxblood red critical states;
- large destination photography;
- occasional serif display typography;
- simple line icons;
- subtle texture only in editorial areas.

## Avoid

- glassmorphism;
- neon gradients;
- excessive shadows;
- excessive rounded cards;
- random travel stickers;
- airline-app appearance;
- corporate dashboard appearance;
- luxury-travel branding.

---

# 4. Color system

The palette is intentionally restrained.

## 4.1 Core brand colors

| Token | Value | Role |
|---|---|---|
| `ink` | `#16232E` | Primary text, dark UI |
| `paper` | `#F5F1E8` | Main app background |
| `surface` | `#FFFDF8` | Elevated/readable surface |
| `teal` | `#1F6F78` | Primary action |
| `gold` | `#B8863B` | Editorial accent |
| `oxblood` | `#7A2E2E` | Critical / urgent |
| `moss` | `#4C6444` | Confirmed / success |

### Accessibility baseline

Recommended combinations:

- white on teal;
- white on oxblood;
- white on moss;
- ink on paper;
- ink on surface;
- ink on gold.

All are intended to meet WCAG AA for normal text when used at full token values.

---

# 5. Neutral palette

| Token | Value |
|---|---|
| `neutral-950` | `#16232E` |
| `neutral-800` | `#33414B` |
| `neutral-700` | `#4B5962` |
| `neutral-600` | `#65717A` |
| `neutral-500` | `#7E898F` |
| `neutral-400` | `#A4ACA9` |
| `neutral-300` | `#C8CCC5` |
| `neutral-200` | `#DDDCD4` |
| `neutral-100` | `#ECE9E1` |
| `neutral-050` | `#F5F1E8` |
| `white` | `#FFFDF8` |

Do not use pure black as the primary text color.

---

# 6. Semantic colors

Semantic colors always communicate state, never decoration.

## 6.1 Primary

- foreground: `#FFFFFF`
- background: `#1F6F78`
- subtle background: `#E4F0F0`
- subtle foreground: `#185860`

Use for:

- primary actions;
- selected state;
- active tour;
- navigation emphasis;
- links when visually appropriate.

## 6.2 Success / confirmed

- foreground: `#FFFFFF`
- background: `#4C6444`
- subtle background: `#E7EDE3`
- subtle foreground: `#3D5337`

Use for:

- reservation confirmed;
- completed checklist;
- participant synchronized;
- completed travel item.

## 6.3 Warning / verify

- foreground: `#16232E`
- background: `#B8863B`
- subtle background: `#F3E8D0`
- subtle foreground: `#684817`

Use for:

- verify locally;
- uncertain timing;
- minor operational attention;
- outdated live information.

## 6.4 Critical

- foreground: `#FFFFFF`
- background: `#7A2E2E`
- subtle background: `#FDECEC`
- subtle foreground: `#7A2E2E`

Use for:

- boarding deadline;
- check-in deadline;
- "cannot go wrong";
- emergency;
- destructive actions.

Do not use critical red for ordinary errors that do not endanger the trip.

## 6.5 Information

Use the primary teal family by default.

---

# 7. Status taxonomy

Travel-specific status is part of the design system.

## Booking / transport states

### Confirmed
Semantic: success

Label examples:

- Confirmado
- Pago
- Reservado
- Emitido

### Included
Semantic: neutral / teal subtle

### Buy
Semantic: warning

Label:

- Comprar

### Verify
Semantic: warning

Label:

- Verificar

### Critical
Semantic: critical

Critical is not mutually exclusive with the other states.

Example:

> Reservado + Check-in fecha às 16:00

---

# 8. Typography

Use two families with clearly separated responsibilities.

## 8.1 Operational typeface

**Roboto**

Use for:

- navigation;
- buttons;
- timelines;
- time;
- tickets;
- weather;
- warning states;
- forms;
- metadata;
- captions that affect action.

Reason:

- native Android familiarity;
- excellent readability;
- no visual ambiguity;
- strong numeric rendering.

## 8.2 Editorial typeface

**Fraunces**

Use sparingly for:

- city names;
- chapter titles;
- attraction hero titles;
- major editorial statements.

Never use for:

- critical warnings;
- times;
- transport identifiers;
- ticket data;
- buttons.

---

# 9. Type scale

## Display

### Display Large
Fraunces SemiBold  
40sp / 44sp

Use:
- destination hero;
- major city page.

### Display Medium
Fraunces SemiBold  
32sp / 38sp

### Display Small
Fraunces SemiBold  
28sp / 34sp

---

## Headings

### Heading 1
Roboto Bold  
28sp / 34sp

### Heading 2
Roboto Bold  
24sp / 30sp

### Heading 3
Roboto Medium  
20sp / 26sp

---

## Body

### Body Large
Roboto Regular  
18sp / 27sp

Use for long editorial reading.

### Body
Roboto Regular  
16sp / 24sp

Default body.

### Body Small
Roboto Regular  
14sp / 20sp

---

## UI

### Label Large
Roboto Medium  
16sp / 20sp

### Label
Roboto Medium  
14sp / 18sp

### Label Small
Roboto Medium  
12sp / 16sp

---

## Time

### Time Hero
Roboto Bold  
28sp / 30sp  
Tabular numerals when available.

### Time Inline
Roboto Bold  
16sp / 20sp

Time should be visually scannable before labels.

---

# 10. Text hierarchy rules

Operational card order:

1. time/status;
2. title;
3. critical fact;
4. description;
5. actions.

Editorial content order:

1. visual;
2. location/date;
3. title;
4. short lead;
5. story;
6. supporting information.

Do not bury critical timing inside paragraphs.

---

# 11. Spacing

Base unit: **4dp**

| Token | Value |
|---|---:|
| `space-0` | 0 |
| `space-1` | 4dp |
| `space-2` | 8dp |
| `space-3` | 12dp |
| `space-4` | 16dp |
| `space-5` | 20dp |
| `space-6` | 24dp |
| `space-8` | 32dp |
| `space-10` | 40dp |
| `space-12` | 48dp |
| `space-16` | 64dp |

## Default screen gutter

**20dp**

Use 16dp only for very dense operational surfaces.

---

# 12. Shape

Rounded corners should feel practical, not playful.

| Token | Value |
|---|---:|
| `radius-xs` | 6dp |
| `radius-sm` | 10dp |
| `radius-md` | 14dp |
| `radius-lg` | 20dp |
| `radius-xl` | 28dp |
| `radius-pill` | 999dp |

## Usage

- button: 14dp;
- operational card: 14dp;
- document preview: 10dp;
- image hero: 20–28dp;
- chips: pill;
- notification-style critical card: 14dp.

Avoid applying the same large radius to every container.

---

# 13. Borders

Default border:

- 1dp;
- `neutral-200`.

Strong divider:

- 1dp;
- `neutral-300`.

Critical card:

- 2dp left or top accent;
- oxblood.

Do not rely on borders alone to communicate status.

---

# 14. Elevation

Keep elevation subtle.

## Level 0
No shadow.

Use for:
- page background;
- inline sections.

## Level 1
Very subtle elevation.

Use for:
- standard card;
- bottom navigation;
- compact player.

## Level 2
Use for:
- floating audio controls;
- active voice recorder;
- temporary contextual panel.

## Level 3
Use only for:
- modal;
- bottom sheet;
- emergency overlay.

Avoid deep Material-style shadows on every card.

---

# 15. Icons

Style:

- simple outlined icons;
- rounded joins;
- consistent stroke;
- Material Symbols Rounded is acceptable for V1.

Icon sizes:

- 18dp compact metadata;
- 20dp inline;
- 24dp default action;
- 28–32dp prominent action;
- 40dp emergency / recorder hero.

Do not mix multiple icon families.

---

# 16. Photography

Photography is part of the information architecture.

## Hero imagery

Use for:

- city;
- attraction;
- walking tour.

Aspect ratios:

- `4:3` default;
- `16:9` cinematic section;
- portrait imagery only inside gallery.

## Treatment

- no heavy color overlays;
- no gradient unless required for text readability;
- preserve natural travel colors;
- subtle paper-like frame/background is acceptable.

## Text on photography

Avoid important operational text directly on imagery.

City name may overlay photography only when contrast is guaranteed.

---

# 17. Illustration and texture

Texture is optional and restricted to editorial surfaces.

Allowed:

- subtle grain;
- map-like line work;
- field-guide marks;
- route lines.

Not allowed:

- texture behind dense operational text;
- texture in documents;
- texture in emergency screens;
- decorative stickers competing with content.

---

# 18. Layout grid

Android phone baseline:

- screen gutter: 20dp;
- 4dp spacing grid;
- single-column primary layout.

Operational screens should be predominantly single column.

Do not create dashboard-style multi-column layouts on phones.

---

# 19. Touch targets

Minimum target:

**48 × 48dp**

Preferred for travel-critical actions:

**52–56dp height**

Examples:

- Show ticket;
- Open Maps;
- Start walk;
- Record memory;
- Emergency call.

---

# 20. Buttons

## 20.1 Primary button

Background: teal  
Text: white  
Height: 52dp  
Radius: 14dp

Use once per decision surface when possible.

Examples:

- Começar passeio
- Mostrar passagem
- Gravar memória

## 20.2 Secondary button

Background: transparent or surface  
Border: neutral-300  
Text: ink

## 20.3 Tonal button

Background: teal subtle  
Text: teal dark

Useful for:

- audio guide;
- Maps;
- details.

## 20.4 Critical button

Background: oxblood  
Text: white

Reserved for true urgent/emergency actions.

## 20.5 Text action

For low-emphasis secondary actions.

---

# 21. Action hierarchy

Example attraction:

1. **Ouvir audioguia** — primary/tonal depending context
2. **Maps** — secondary
3. **Ingresso** — secondary
4. Site oficial — text

Example transport under time pressure:

1. **Mostrar passagem** — primary
2. **Abrir embarque no Maps** — secondary
3. Operadora — text

The primary action changes based on context.

---

# 22. Chips

Use chips for:

- status;
- category;
- country;
- UNESCO;
- difficulty;
- offline availability.

Do not use chips for full sentences.

Maximum recommended simultaneous chips in a card:

**3**

---

# 23. Navigation

Bottom navigation:

1. Hoje
2. Viagem
3. Explorar
4. Carteira
5. Mais

## Selected state

- teal foreground;
- subtle teal background or indicator;
- icon + text always visible.

Do not hide labels.

---

# 24. Top app bar

Two forms.

## Contextual

Used on root screens.

Examples:

- current location;
- trip day;
- sync/offline indicator.

## Detail

Used on city, attraction, hotel, transport.

Includes:

- back;
- contextual secondary actions.

Avoid overcrowding the app bar.

---

# 25. Core card system

Cards are semantic, not generic containers.

## 25.1 Timeline card

Contains:

- time;
- category;
- title;
- status;
- essential detail;
- optional actions.

## 25.2 Critical card

Contains:

- explicit warning icon;
- concrete consequence;
- deadline;
- immediate actions.

## 25.3 Attraction card

More visual.

Contains:

- image;
- title;
- short descriptor;
- scheduled time;
- audio/Maps indicators.

## 25.4 Transport card

Dense and operational.

Contains:

- origin/destination;
- departure/arrival;
- operator;
- status;
- ticket shortcut.

## 25.5 Accommodation card

Contains:

- check-in/out;
- address;
- reservation status;
- voucher shortcut.

## 25.6 Story card

Small editorial unit.

Contains:

- title;
- short hook;
- listen/read action.

## 25.7 Plan B card

Muted warning styling.

Plan B should feel reassuring and actionable, not alarming.

---

# 26. Timeline

The day timeline is a core branded component.

## Structure

```text
08:00
│
●  Transporte
│  Ksamil → Butrinto
│
08:30
│
●  Atração
│  Butrinto
│
...
```

Rules:

- time has consistent fixed alignment;
- current item receives stronger marker;
- completed items de-emphasize;
- critical items may use oxblood marker;
- active item may use teal marker.

Do not overuse card backgrounds around every timeline item.

---

# 27. “Agora” component

The “Agora” card is the strongest operational element on Home.

Must show at a glance:

- what;
- where;
- until when / when next;
- one primary action.

Optional:

- contextual audio;
- critical warning;
- map.

It must visually differ from the rest of the timeline.

---

# 28. Critical Item component

Visual anatomy:

1. critical icon;
2. “Não pode dar errado” label;
3. action/event;
4. deadline;
5. reason;
6. immediate buttons.

Example:

> **Não pode dar errado**
>
> Ônibus para Budva · 19:30  
> Esteja no ponto até 19:00.
>
> [Mostrar passagem] [Maps]

---

# 29. Weather component

Weather should be compact.

Home displays:

- condition;
- current temperature;
- min/max;
- rain.

Detailed page may show hourly information.

Freshness must always be visible when data is stale.

Example:

> Atualizado há 2h

Never present historical fallback as live weather.

---

# 30. Outfit component

Use simple categorized recommendations.

### Vista
- camiseta
- shorts
- tênis

### Leve
- repelente
- protetor

Avoid fashion-oriented visual treatment.

This is functional packing advice.

---

# 31. Audio player

Audio is a first-class product surface.

## Compact player

Persistent at bottom while playing.

Displays:

- title;
- play/pause;
- progress;
- group state when applicable.

## Full player

Displays:

- image;
- title;
- chapter;
- progress;
- ±15s;
- previous/next;
- transcript;
- group participants.

## Audio semantic color

Primary teal.

Do not use a unique unrelated purple/music color.

---

# 32. Group audio state

Participant states:

### Synchronized
Moss indicator

### Connecting
Gold indicator

### Offline
Neutral indicator

### Out of sync
Gold indicator

Do not show technical latency numbers in the normal UI.

User-facing terminology:

- Sincronizado
- Conectando
- Offline
- Sincronizando novamente

---

# 33. Walk Mode UI

When the walk is active, prioritize:

1. current story;
2. next instruction;
3. audio status;
4. group presence;
5. exit/pause.

The phone should not need to remain visible.

## Active walk header

- tour name;
- progress: `3 de 7`;
- approximate remaining distance/time.

## GPS state

Only communicate when relevant:

- localização ativa;
- localização fraca;
- waiting for location.

Avoid permanent technical GPS telemetry.

---

# 34. Story trigger

When a story becomes available during a walk:

### Audio-first behavior

If automatic playback is enabled:
- play a subtle earcon;
- narrate.

If not:
- notification / compact prompt.

Do not use loud notification sounds that interrupt the environment.

---

# 35. Voice recorder

Voice memory recording is intentionally minimal.

## Idle

Large action:

**Gravar memória**

## Recording

- elapsed time;
- waveform / simple amplitude;
- pause;
- finish;
- cancel.

Critical touch actions must be spaced apart.

## Saved state

Brief confirmation:

> Memória salva em Žabljak · 14:51

No mandatory title or text entry.

---

# 36. Documents / Wallet

Document UI favors speed over beauty.

## Document row

- type icon;
- name;
- relevant date;
- participant if applicable;
- offline indicator.

## Document viewer

Top actions:

- close;
- share/open externally.

Context action:

- QR mode if available.

## QR mode

- maximum brightness;
- keep screen awake;
- large QR;
- minimum surrounding UI.

---

# 37. Emergency UI

Emergency surfaces deliberately break the normal visual softness.

Use:

- high contrast;
- large text;
- clear phone buttons;
- minimal imagery;
- zero editorial decoration.

Primary emergency action may use oxblood.

Example:

**112 — Emergência**

Button:
**Ligar 112**

Never make users parse paragraphs before the action.

---

# 38. Plan B UI

Plan B uses a calm, practical tone.

Recommended treatment:

- warm gold subtle background;
- clear scenario title;
- 1–3 next actions.

Example:

### Se perder o ônibus

1. Vá até a rodoviária.
2. Verifique a próxima saída para Kotor.
3. Se não houver, abra a opção de hospedagem emergencial.

Plan B should not look like an emergency unless the scenario actually is one.

---

# 39. Deeplink buttons

Use recognizable service names/icons only when legally/visually appropriate.

Examples:

- Maps
- Bolt
- Uber
- Booking

The label should describe the action, not just the product:

- Abrir no Maps
- Chamar Bolt
- Ver reserva no Booking

---

# 40. Offline indicator

Do not show a permanent alarming red banner.

Default behavior:

Small status in appropriate context.

Example:

> Offline · conteúdo disponível

When live-only data is affected:

> Sem internet · previsão não atualizada

---

# 41. Loading states

Prefer:

- skeleton only for live content;
- immediate local rendering for trip content.

The itinerary should never display a loading spinner just because synchronization is happening.

---

# 42. Empty states

Empty states should be brief and contextual.

Examples:

### Sem memórias ainda

> Quando quiser registrar este momento, toque em Gravar memória.

### Sem previsão

> A previsão ainda não foi atualizada. O planejamento climático da viagem continua disponível.

---

# 43. Error language

Errors should explain what continues to work.

Poor:

> Erro de conexão.

Preferred:

> Não foi possível sincronizar o grupo. Seu audioguia continua funcionando normalmente.

---

# 44. Notifications

Notification hierarchy:

## Operational
High relevance.

Examples:
- leave now;
- check-in closing;
- boarding approaching.

## Walk story
Normal relevance.

## Group synchronization
Low/normal relevance.

## Voice journal reminder
Low relevance.

Notifications should always contain a useful action when possible.

---

# 45. Motion

Motion should be restrained.

## Durations

- micro: 120ms
- standard: 200ms
- emphasized: 300ms

## Allowed

- card expansion;
- audio player transition;
- timeline current marker;
- recorder waveform;
- participant connection;
- 3–2–1 synchronized start.

## Avoid

- decorative page transitions;
- parallax during operational tasks;
- bouncing buttons;
- long travel-themed animations.

Respect Android reduced-motion preferences.

---

# 46. Haptics

Use selectively.

Recommended:

- start/stop recording;
- critical confirmation;
- synchronized walk start;
- checklist completion.

Avoid haptic feedback on every tap.

---

# 47. Accessibility

Baseline requirements:

- WCAG AA contrast;
- minimum 48dp touch target;
- text scaling support;
- no color-only state;
- meaningful content descriptions;
- predictable focus order;
- captions/transcripts for all audioguides;
- vibration/haptic not required to understand state.

All audioguides should have a transcript.

---

# 48. Sunlight mode

No separate mode is necessary in V1.

Instead, default light theme already optimizes for outdoors:

- light warm background;
- dark high-contrast text;
- strong button contrast.

Avoid low-contrast gray-on-gray combinations.

---

# 49. Night theme

Night mode should exist eventually, but the first visual exploration should prioritize the light theme.

Suggested foundation:

| Role | Value |
|---|---|
| background | `#10171C` |
| surface | `#182128` |
| elevated | `#202C34` |
| primary text | `#F2EFE7` |
| secondary text | `#BCC4C7` |
| teal | `#67B6BB` |
| gold | `#D5AB68` |
| moss | `#91AE84` |
| critical | `#E18B84` |

Do not simply invert all colors.

---

# 50. Component naming

Recommended implementation naming:

```text
TcButton
TcIconButton
TcChip
TcStatusChip
TcCard
TcTimeline
TcTimelineItem
TcNowCard
TcCriticalCard
TcWeatherSummary
TcOutfitCard
TcCityHero
TcAttractionHero
TcTransportCard
TcAccommodationCard
TcStoryCard
TcPlanBCard
TcAudioPlayer
TcGroupAudioStatus
TcParticipantStatus
TcWalkProgress
TcDocumentRow
TcQrViewer
TcVoiceRecorder
TcEmergencyAction
TcOfflineStatus
TcSectionHeader
```

`Tc` = Travel Companion.

---

# 51. Component state matrix

All interactive components should define:

- default;
- pressed;
- focused;
- disabled;
- loading when applicable;
- offline when applicable;
- error when applicable.

Travel-specific components additionally define:

- upcoming;
- active;
- completed;
- critical.

---

# 52. Content style

## Interface language

Portuguese (Brazil) for the reference trip.

## Voice

Concise, useful and human.

Preferred:

> Esteja no ponto até 19:00.

Avoid:

> Recomendamos que os usuários se dirijam ao ponto de embarque com antecedência adequada.

## Editorial content

May be more narrative.

## Critical content

Must be literal and unambiguous.

---

# 53. Number and time formatting

Reference locale:

`pt-BR`

Examples:

- `19:30`
- `16 de setembro`
- `€ 24,07`
- `1.000 lek`
- `2,5 km`
- `45 min`

Transport codes retain original operator formatting.

---

# 54. Country and location styling

Flag emojis may be used as supportive visual elements, not as the only country identifier.

Preferred:

> Sarajevo · Bósnia e Herzegovina 🇧🇦

Do not overload every card with flags.

---

# 55. Maps language

Map actions should use consistent labels:

- Abrir no Maps
- Como chegar
- Ver ponto de embarque
- Ver hospedagem

Avoid generic labels like:

- Link
- Abrir
- Ver

---

# 56. Audio language

Consistent vocabulary:

- Audioguia
- Ouvir
- Ouvir juntos
- Pausar
- Continuar
- Próxima história
- Transcrição
- Sincronizado

Avoid technical terms like:

- stream;
- session;
- room;
- peer;
- timestamp.

---

# 57. GPS language

User-facing:

- Histórias pelo caminho
- Localização ativa
- Procurando sua localização
- Localização imprecisa

Avoid:

- geofence;
- background location;
- location provider.

Those concepts belong only in permission explanations where necessary.

---

# 58. Permission explanation pattern

Permissions must be explained before Android requests them.

Example:

### Histórias pelo caminho

> Durante os passeios, o app pode avisar quando vocês chegarem perto de um lugar interessante, mesmo com a tela apagada.

Then request location permission.

Explain the user benefit first.

---

# 59. Design QA checklist

Before approving a screen:

- [ ] Can the next relevant action be identified in under 5 seconds?
- [ ] Is any critical time visible without opening more details?
- [ ] Are all tap targets at least 48dp?
- [ ] Does the screen work without color?
- [ ] Does editorial styling stay away from operational ambiguity?
- [ ] Is offline behavior understandable?
- [ ] Are primary and secondary actions visually distinct?
- [ ] Is there unnecessary card nesting?
- [ ] Does it still work with larger text?
- [ ] Would it be understandable outdoors and while walking?

---

# 60. First components to design visually

Before full screen design, create visual specimens for:

1. Primary / secondary / tonal / critical buttons
2. Status chips
3. Timeline item
4. Now card
5. Critical item
6. Attraction card
7. Transport card
8. Audio compact player
9. Group participant status
10. Walk progress
11. Voice recorder
12. Document row
13. Emergency action
14. Plan B card

These components should then be exercised in the first three screen concepts:

- Hoje
- Cidade
- Modo Passeio

---

# 61. Design system decision summary

The system deliberately separates:

## Editorial identity

- Fraunces
- photography
- paper tone
- gold details
- storytelling
- breathing room

from:

## Operational identity

- Roboto
- teal actions
- explicit semantic status
- timelines
- high contrast
- compact information

The two meet through:

- shared spacing;
- shared color tokens;
- shared iconography;
- shared component geometry;
- consistent interaction patterns.

This is the foundation that should be used by the Travel Companion design specification and all future UI exploration.
