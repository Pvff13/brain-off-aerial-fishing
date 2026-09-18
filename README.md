# Brain off Aerial Fishing

Highlights the aerial fishing spot (Lake Molch) you can catch from soonest, ranked by zone
rather than raw tile distance - a spot 3 tiles away is exactly as good as one 4 tiles away,
since both fall in the same "2 tick" zone. Frenzied spots are always ranked as the 3-tick
zone regardless of their real distance, since their catch cycle is a flat 3 ticks once
reached: a 1- or 2-tick spot still wins over a frenzied spot, but a frenzied spot wins over
a plain 3-tick spot. Whenever two spots are tied after all of that, the one that's been
around the longest is highlighted instead, since it's the one closest to disappearing.

A spot that's estimated not to survive the round trip - i.e. it'd probably despawn before
the cormorant could get there and catch - drops behind every spot that would survive it, so
the highlight moves on to the next best option rather than sending you somewhere that's
about to vanish.

This is an overlay-only plugin: it observes NPCs and draws on top of the scene. It never
clicks, moves the mouse, or interacts with anything.

It can also separately remind you to reboost Fishing and/or Hunter - see
[Reboost reminders](#reboost-reminders) below.

## Features

- Tracks every regular and frenzied fishing spot around you and ranks them by zone, with
  frenzied spots slotted into the shared 3-tick zone and oldest-first as the final
  tie-breaker
- Highlights the best spot to fish next, with optional highlighting of the 2nd and 3rd best
  too - each with its own colour
- Skips over a spot that's estimated to despawn before you could catch from it, moving the
  highlight to the next viable option instead
- Optional travel-ticks readout above each highlighted spot
- Optional estimated-time-left countdown above each highlighted spot, which turns red once
  the spot's been up for 10+ ticks (the low end of its observed lifespan - it could
  realistically be gone already), and can be worded as "~7 ticks left" or just "~7"
- Adjustable assumed lifespan (10-19 ticks) driving both that countdown and the
  survives-the-round-trip check
- Separate "Show frenzied spot timer" toggle for frenzied spots specifically, since their
  lifespan is a known fixed 28 ticks rather than a randomised range - shown independently of
  the general countdown above, and always used (regardless of that toggle) for the
  survives-the-round-trip check on frenzied spots
- "Always show frenzied spots" toggle to keep every tracked frenzied spot visible (hull plus
  any enabled ticks/timer text) at all times, even when it isn't your best, 2nd, or 3rd best
  pick right now - useful for keeping an eye on one while you fish something closer instead
- Optional faint outline on every other tracked spot, so you can see the full picture
- Toggle for whether frenzied spots are eligible to be picked at all
- Customisable colours, with an option to fill each spot's whole clickbox in its colour
  instead of just outlining it
- Optional catches-per-hour stats panel - see [Catches-per-hour tracker](#catches-per-hour-tracker)
  below

## Notes on tick timing

The tile-distance-to-zone breakpoints (as observed in-game) are: 1-2 tiles away = 1 tick,
3-4 tiles = 2 ticks, 5 tiles = 3 ticks, 6-7 tiles = 4 ticks, 8-9 tiles = 5 ticks, and it
caps at 6 ticks for anything further - it doesn't keep climbing indefinitely. Frenzied
spots ignore this table entirely and are always treated as the 3-tick zone.

## Notes on the lifespan estimate

A fishing spot's actual lifespan isn't documented on the wiki and isn't directly observable
(there's no visible countdown in-game). It's been observed staying up anywhere from 10 to
19 ticks, per the [aerial-fishing-timers](https://github.com/call-me-maple/aerial-fishing-timers)
plugin, which was built specifically to time this. Both the "estimated time left" readout
and the "will it survive the round trip" check are built on a single assumed value within
that range (14 by default, adjustable in settings) - raise it for a more optimistic
estimate, lower it for a more cautious one. Either way, treat this as a rough, best-effort
estimate rather than an exact timer - a spot's real lifespan is randomised per spawn and
can't be known in advance. As a secondary warning independent of that setting, the
countdown text turns red once 10 ticks (the low end of the range) have actually elapsed,
since the spot could realistically already be gone by then.

## Configuration

All colours and toggles are configurable from the plugin's settings panel.

## Notes on "oldest"

There's no way to observe a spot's true spawn time before this plugin starts watching it,
so any spots already on Lake Molch when you log in (or when you enable the plugin) are
treated as tied with each other for "oldest" - their relative order between themselves is
arbitrary, though they'll still correctly tie-break as older than anything that spawns
afterwards.

## Reboost reminders

Separate from the fishing spot highlighter above, this plugin can remind you to reboost
Fishing and/or Hunter once your current boost on that skill (how many levels above your
base level you currently are) drops below a threshold you set. Fishing and Hunter are
independent settings with their own toggle, threshold, and colour, since people boost each
skill differently. Both are off by default.

Each reminder only ever shows up while you're actually wearing the
[Cormorant's glove](https://oldschool.runescape.wiki/w/Cormorant%27s_glove) (i.e. actually
aerial fishing) - it never nags you about Fishing/Hunter boosts while you're off doing
something unrelated elsewhere in the game.

Three independent display options - Infobox and Highlight boost items are on by default once
a reminder is enabled, Send chat message is off by default:

- **Infobox** - pick a style: **Icon + boost margin** shows the usual small infobox with the
  skill's icon and your current boost margin (e.g. "+1"), with your custom text as its
  tooltip; **Custom text** shows a proper panel instead (an infobox that size isn't built to
  hold a sentence), with your custom text on the left and the boost margin on the right,
  both always visible with no hovering needed for either
- **Send chat message** - sends your custom text as a chat message once, the exact moment
  your boost crosses below the threshold (not repeated every tick while it stays low)
- **Highlight boost items** - highlights anything in your inventory or worn that can
  reboost that skill, in a colour you choose

The full list of items each reminder recognises (transcribed from every source listed on
the wiki's [Temporary skill boost](https://oldschool.runescape.wiki/w/Temporary_skill_boost)
page, as of 2026-09-09):

- **Fishing**: Fishing potion, Super fishing potion, Fishing cape (untrimmed/trimmed),
  Barnacle blaster, Trawler's trust, Dragon/Infernal/Crystal harpoon (via their
  "Fishstabber" special attack), Fishing mix, Fish pie, Bottle of fishtongue tonic,
  Admiral pie
- **Hunter**: Hunter potion, Super hunter potion, Hunter cape (untrimmed/trimmed),
  Blackbird red, Sailor's mirage, Trapper's tipple, Hunting mix

Two sources from that wiki page are deliberately left out: Spicy stew (every spice colour
shares one item id, and the effect is a random +0-5 that can just as easily be a drain, so
an inventory Spicy stew can't be identified as "a Fishing/Hunter booster"), and the
"invisible" boosts from the Fishing Guild and Horn of Plenty (documented as only affecting
success-rate rolls, never the visible boosted level this feature actually watches).

## Catches-per-hour tracker

An optional draggable stats panel with "Caught fish:" and "Fish/hr:" rows. Off by default.

Catches are detected the same way RuneLite's own built-in Fishing plugin detects them - the
chat message your cormorant sends back ("Your cormorant returns with its catch."), not an
xp-based guess - so it's exact regardless of which fish (and therefore how much xp) it was.

Counting itself always runs in the background while you're wearing the Cormorant's glove,
regardless of whether the panel is currently shown - so turning the panel on mid-session
immediately reflects the whole session rather than starting over from zero. The rate is
measured from your *first* catch, not from whenever the glove went on, so standing around
beforehand doesn't drag it down. Everything resets when you log out, hop worlds, disable the
plugin, or - matching RuneLite's own built-in Fishing plugin - go 5 minutes without a catch,
so the panel doesn't linger showing a rate that no longer reflects what you're doing.
