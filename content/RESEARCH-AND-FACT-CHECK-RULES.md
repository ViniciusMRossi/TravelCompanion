# Travel Companion — Research and Fact-Check Rules

These rules apply to every AI-generated trip content package.

---

# 1. Research goal

Research exists to make the app trustworthy while traveling.

The goal is not to maximize the quantity of information.

Prioritize information that affects:

- getting somewhere;
- entering somewhere;
- timing;
- safety;
- understanding what the traveler is seeing;
- recovering when a plan fails.

---

# 2. Source hierarchy by domain

## Booking / ticket / accommodation facts

Preferred order:

1. user-provided booking/ticket/voucher;
2. official operator/property communication;
3. official booking platform record;
4. official operator/property website;
5. secondary source only for non-critical supporting detail.

Never replace booking-document facts based only on a search snippet or travel blog.

---

## Transport schedules and boarding information

Preferred order:

1. actual ticket/reservation;
2. official operator;
3. official station/airport/port;
4. authoritative timetable provider;
5. reputable secondary source.

Schedules are volatile.

Record:

- source;
- date checked;
- travel date the information applies to;
- confidence.

If current schedule cannot be confirmed, say so.

---

## Attraction operations

Use official venue / heritage / municipality / national-park sources where available.

Verify:

- opening dates;
- opening hours;
- ticket rules;
- seasonal restrictions;
- reservation requirement;
- important closures.

If the trip is in the future and hours may change, flag the information as needing a closer-to-trip re-check.

---

## Coordinates

Preferred:

1. official geographic/map reference;
2. reputable mapping/open-data source;
3. cross-check with a second map when a location trigger depends on precision.

For automatic story triggers, coordinate confidence should normally be HIGH.

Do not estimate coordinates from a city name.

---

## Emergency information

Use official government, emergency-service, consular or insurer sources.

Emergency information requires HIGH confidence.

Never use an unverified blog/listicle as the sole source.

Never infer a number from another country.

---

## Historical/editorial research

Prefer:

- museum/heritage institution;
- official cultural institution;
- academic source;
- reputable encyclopedia;
- serious historical publication;
- credible tourism authority for practical context.

Avoid using a single tourism marketing page as the sole basis for significant historical claims.

---

## Restaurants

Use current sources.

Useful evidence may include:

- official site/social profile;
- reliable maps/business listing;
- current booking platform;
- recent reputable reviews.

Restaurants are recommendations, not guaranteed operations.

Record last checked date.

---

# 3. Freshness classes

Classify researched information by volatility.

## Very volatile

Re-check close to travel:

- transport schedule;
- platform/gate;
- restaurant opening;
- temporary closures;
- attraction opening hours;
- ticket price;
- weather.

## Moderately volatile

- phone/contact;
- official booking rules;
- consular location;
- app availability.

## Stable

- historical narrative;
- monument location;
- geography;
- architectural observation.

Even stable facts can be wrong, but they usually do not need same-day verification.

---

# 4. Evidence record

`research/SOURCES.md` should contain one evidence block per meaningful entity or topic.

Template:

```text
## entity-id / claim group

Purpose:
What this evidence supports.

Source:
<source name>

URL / document:
<reference>

Checked:
YYYY-MM-DD

Source type:
USER_DOCUMENT | OFFICIAL | INSTITUTIONAL | SECONDARY | MAP | OTHER

Confidence:
HIGH | MEDIUM | LOW | UNVERIFIED

Volatility:
VERY_HIGH | MEDIUM | LOW

Notes:
Any conflict, limitation, date applicability or ambiguity.
```

User documents may be identified by local filename rather than copied into the report.

---

# 5. Fact versus recommendation

Do not blur these categories.

Example:

```text
FACT
Bus departure: 19:30
Source: ticket

DERIVED RECOMMENDATION
Be at station by: 19:00
Reason: 30-minute operational margin
```

Never write:

> The bus company requires you to arrive at 19:00

unless a source actually says so.

---

# 6. Conflict handling

For each conflict record:

```text
## Conflict ID

Entity:
transport.x

Field:
departure time

Source A:
ticket — 19:30

Source B:
official timetable — 20:00

Precedence:
ticket currently wins because it is the actual user booking

Risk:
HIGH

Required action:
Verify with operator before production/travel.
```

Do not silently average, merge or choose based on convenience.

---

# 7. Historical uncertainty

Some local stories have multiple versions.

When the evidence is disputed:

- say that it is disputed;
- avoid presenting folklore as established fact;
- use phrases such as "uma versão local conta..." only when that distinction improves the story;
- omit weak claims when they add little value.

Interesting does not outrank accurate.

---

# 8. Generated factual prose

Before finalizing editorial prose, verify:

- dates;
- people;
- causal historical claims;
- superlatives ("oldest", "largest", "first");
- distances;
- counts;
- religious/ethnic/historical labels;
- war/conflict claims.

Superlatives are especially error-prone and should be omitted unless well supported.

---

# 9. Sensitive/personal source material

User documents may contain:

- passport information;
- policy numbers;
- reservation codes;
- phone numbers;
- addresses.

Rules:

- use only what is necessary for the app;
- do not quote full sensitive documents into research reports;
- do not publish secrets in general-purpose logs;
- do not upload documents to unrelated services;
- do not invent or "repair" unreadable identifiers.

---

# 10. Production acceptance rules

Before production:

- all travel-critical facts HIGH or explicitly human-approved;
- all emergency contacts HIGH;
- no unresolved high-risk conflict;
- no operational placeholder;
- all automatic story coordinates HIGH;
- all time-sensitive facts have timezone context;
- volatile facts have a clear last-checked date;
- known items requiring re-check are visible in the generation report.
