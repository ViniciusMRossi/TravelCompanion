# First Prompt for Claude Code

Open Claude Code in the repository root and send:

> Read `CLAUDE.md` and `docs/START-HERE.md` completely before making changes.
>
> First verify the repository baseline:
>
> 1. run `python tools/check_repo.py`;
> 2. validate `app/src/main/assets/trip/trip.json` against `trip-package/schema/trip.schema.json`;
> 3. sync/build the Android project;
> 4. run the current tests;
> 5. inspect the approved visual prototype under `docs/design/prototype/`.
>
> Fix only bootstrap/build issues if necessary. Do not redesign or expand scope.
>
> Once the baseline is clean, complete **Phase 0** from `docs/technical/TECHNICAL-IMPLEMENTATION-BRIEF-v1.md`.
>
> Then implement **Phase 1** in this order:
>
> - Screen 01 — Quem é você?
> - Screen 02 — Hoje
> - Screen 05 — Atração
> - shared Field Companion components needed by those screens
>
> Match the approved Claude prototype faithfully. Do not treat the current placeholder Composables as visual references; they are only wiring/data scaffolds.
>
> Keep `docs/IMPLEMENTATION-STATUS.md` updated after each meaningful milestone.
>
> Stop and report before beginning Phase 2, including:
>
> - files changed;
> - tests/builds run;
> - visual fidelity status;
> - any blockers or unresolved implementation decisions.

## Why stop before Phase 2?

The first checkpoint lets us verify that:

- project foundation is healthy;
- the implementation agent is respecting the locked design;
- data-driven UI works correctly;
- the three foundational screens establish reusable components.

After approval, Claude can continue into Media3/audio and the rest of the vertical slice.
