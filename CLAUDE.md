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

This is a **Phase 0 starter**.

Next:

1. Run `python tools/check_repo.py`.
2. Sync/build/test.
3. Complete Phase 0.
4. Implement approved screens 01, 02 and 05.
5. Continue the vertical slice in the implementation brief.

Update `docs/IMPLEMENTATION-STATUS.md` after meaningful milestones.

If a visual question appears, inspect the approved prototype before asking for a new design decision.
