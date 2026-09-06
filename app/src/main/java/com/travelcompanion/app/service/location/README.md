# Location service

The brief asks for two different modes, and this package is the line between
them. Both are live.

| Mode | Mechanism | Runs | Costs |
| --- | --- | --- | --- |
| Active Walk Mode | fused location, `PRIORITY_HIGH_ACCURACY`, 5 s | only while a walk is running | a foreground service and its notification |
| Passive story discovery | Play Services geofences | always, when the permission is there | what the system already spends on location |

`LocationSource` is the first. It is a cold flow: it starts requesting when
Walk Mode collects it and stops the moment collection ends, because the brief
is literal that high-accuracy GPS must not run outside Walk Mode.

`StoryGeofences` is the second, and is the reason the second may exist at all.
A geofence is not a location flow of ours: the circles are watched by the
system with the location it is already spending, and the app is woken once.

## The geofence is an alarm clock, not a judge

The one thing to understand before changing anything here.

- the three real triggers declare **80 m**, and 80 m is what decides;
- Android geofencing is unreliable below ~100 m, so the circles are
  **registered** at `GEOFENCE_FLOOR_RADIUS_METERS` = **120 m**;
- when a circle rings, the position that came with the wake-up goes through
  `decideStoryTrigger` — the same function Walk Mode uses, at the trigger's own
  80 m;
- so an early wake-up decides **nothing**, and that is the expected answer, not
  an error.

The floor never reaches a decision. If you find yourself comparing a distance
with a radius anywhere in this package, stop: the rule lives in
`domain/walk/StoryTriggering.kt` and a second copy of it is D096's defect
(D103).

`GEOFENCE_TRANSITION_DWELL` is registered beside `ENTER`, with a one-minute
loiter, because crossing the outer rim at 100 m and then walking to the plaque
produces no second `ENTER`. It rings the same alarm again; it does not decide
anything of its own.

## What is registered, and when

Every story that carries a `trigger` and has not yet had its turn in
`StoryTriggerStore` — three circles, all in Sarajevo, against Android's ceiling
of 100 per app. A story's circle comes down as soon as it has spoken:
`notifyOncePerTrip` defaults to true and all three real triggers take the
default, so a circle that has already fired can only cost battery.

Not filtered by day or city, deliberately — see D105 (c) for the filter that
exists and why it is not applied.

Registration happens when the app comes back on screen, and after a boot
through `BootRescheduleReceiver`, which puts these back beside the alarms.
Geofences do not survive a restart.

## Permission

`ACCESS_BACKGROUND_LOCATION` is declared and used only by this. It is asked for
once, from screen 19, and never insisted on: from Android 11 the system shows
no dialog at all, so the app points at Android's own settings page and stops
(D104).

**A refused permission costs the traveller automatic stories, never the walk
and never the audio.** Without it the walk runs whole, the audio plays, and
every story is still on screen 04. Only this does not happen.

## Where the parts are

| File | What it is |
| --- | --- |
| `LocationSource.kt` | fused location for Walk Mode, behind an interface |
| `StoryGeofences.kt` | registration only: which circles, at what radius, and taking them down |
| `PassiveStoryDiscovery.kt` | the wiring — read the record, ask the pure function, notify, cancel |
| `StoryGeofenceReceiver.kt` | the Android plumbing, and nothing else |

`PassiveStoryDiscovery` holds no `Context` on purpose: everything worth testing
is there, and it is driven on the JVM with no Play Services and no device.
