# Claude Code Instructions — Travel Companion

You are implementing the Android **Travel Companion** app.

The product and visual design are already approved.

## Read before changing code

1. `docs/START-HERE.md`
2. `docs/design/SOURCE-OF-TRUTH.md`
3. `docs/design/DESIGN-HANDOFF-v2.md`
4. `docs/design/TELAS-E-FUNCIONALIDADES-APPROVED.md`
5. `docs/design/SCREEN-INDEX-v2.md`
6. `docs/technical/TECHNICAL-IMPLEMENTATION-BRIEF-v1.md`
7. `trip-package/schema/trip.schema.json`

For visual implementation inspect:

- `docs/design/prototype/Travel Companion - Fluxo Principal.dc.html`
- `docs/design/prototype/Travel Companion - Telas Secundárias.dc.html`
- `docs/design/Field-Companion-Component-Gallery-v0.1.html`

The approved prototype is the visual source of truth.

## Do not redesign

Do not:

- change the Field Companion visual direction;
- add login/account creation;
- add pairing, rooms, codes or invitations;
- move trip content into Firebase;
- stream bundled audio;
- replace voice memory with a text-first journal;
- expose Firebase/networking terminology in UI;
- change the 19 canonical screens without explicit approval;
- add generic planner/social scope;
- use default Material appearance as the final UI.

## Product invariants

- Android-first.
- Offline-first.
- Packaged `trip.json` + local assets are authoritative content.
- First launch asks **Quem é você?**
- Participants already belong to the trip group.
- Local audio keeps playing if sync fails.
- Walk Mode assumes headphones + screen off.
- Location-triggered stories are enrichment, not critical navigation.
- Critical timing stays visually separate from editorial content.
- Voice memories require no title or text.

## Engineering style

Prefer:

- one app module;
- ViewModel + StateFlow + Compose;
- small repositories/interfaces;
- deterministic local behavior;
- tests around real runtime risk.

Avoid:

- speculative frameworks;
- generic infrastructure;
- premature module splitting;
- heavyweight SDD/ADR processes.

## Current repository phase

**Phase 8, seven days from departure.** Not a starter: all 19 canonical
screens exist, and §32's Definition of Done stands at 13 of 13 — the last
one was weather's live and cached states, built in 6717f8b and seen on a
Galaxy S24 in f4818d6, the four states in one install.
Real-device QA is marked closed, but it was run against a *debug* build; the
release APK has never been on a phone (D110). That, not new feature work, is
the risk worth spending the remaining days on.

What matters before touching anything:

- the real package ships in `app/src/main/assets/trip-production/`, which is
  **gitignored** — it is not in the repository and cannot be recovered from it;
- the binary for device QA is
  `app/build/outputs/apk/release/app-release.apk`, signed with the debug key
  by decision (D110). The debug build draws a prototype scaffold on screen 07
  and must not be what gets tested;
- the two **tracked** `trip.json` files, `app/src/main/assets/trip/` and
  `trip-package/sample/sample-trip.json`, are the same git blob and have to
  stay that way. `core.autocrlf` is on, so never `git checkout --` them to
  "clean up";
- the verification recipe — validators, Gradle tasks, and what each one is
  expected to say — lives in `docs/IMPLEMENTATION-STATUS.md`. Run it from
  there rather than from memory, and read the counts there rather than here.

The trip runs **13 September to 2 October 2026**. Until it ends, every commit
needs a reason: no refactors, no dependency bumps, no redesign. A change that
cannot name the defect it fixes is a change that can only break something that
currently works.

Update `docs/IMPLEMENTATION-STATUS.md` after meaningful milestones, and record
decisions in `docs/DECISIONS.md`.

If a visual question appears, inspect the approved prototype before asking for a new design decision.
