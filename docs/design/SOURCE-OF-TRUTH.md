# Travel Companion — Source of Truth v2

Status: **DESIGN LOCKED**
Date: 2026-09-01
Platform: Android
Reference viewport: 412 × 915 px

## Precedence

When two artifacts disagree, use this order:

1. **Approved Claude prototype** (`Travel Companion main flow prototype.zip`)
2. **`TELAS-E-FUNCIONALIDADES.md`** extracted from that approved prototype
3. **Field Companion Design System + approved Component Gallery**
4. **This v2 handoff**
5. Earlier v0.1 product/design documents, only for requirements not contradicted above

The implementation agent must not reinterpret or redesign approved visual decisions.

## Locked product decisions

- Personal travel app, not a generic commercial planner.
- Android-first.
- Offline-first.
- Trip content is packaged locally as JSON + assets.
- First launch asks **“Quem é você?”**.
- Participants already belong to the trip group.
- No traditional login, invitation, room, pairing code, or social layer.
- Audio files live locally on each device.
- Group audio synchronizes playback state, not the audio stream.
- Walk Mode is designed to continue with the screen off and the phone in the pocket.
- Stories may be triggered by location.
- Travel memories are voice recordings.
- External specialist apps remain external; Travel Companion opens them contextually.
- Offline is a normal operating mode, not a global error.
- Local playback must never depend on group synchronization.
- Critical timing is always separated from editorial copy.

## Canonical screen count

The approved design has **19 product screens**, plus state boards S1–S5.

The previous handoff referred to 18 screens. That count is obsolete.
