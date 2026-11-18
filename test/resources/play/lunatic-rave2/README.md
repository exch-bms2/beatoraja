# LunaticRave2 single-song comparison

Run `./gradlew regressionLunaticRave2ComparisonTest` with the project's Java
17 toolchain. This test is also part of `regressionTest`. It needs no
window, input device, audio files, or LR2 installation.

| Fixture | Key 1 note times | Other notes |
| --- | --- | --- |
| `boundaries.bms` | 2109.375 ms | Key 7 at 3750 ms |
| `dense.bms` | 2000 and 2125 ms | Key 7 at 4000 ms |
| `long-notes.bms` | 2000 to 3000 ms (`#LNOBJ`) | Scratch at 4000 to 5000 ms (channel 56), key 7 at 6000 ms |
| `mines.bms` | Mines at 2000 and 3000 ms; playable note at 4000 ms | Key 2 at 1000 ms, scratch mine at 3000 ms, key 7 at 6000 ms |
| `5key.bms` | Keys 1/5 | Long scratch and key/scratch mines |
| `9key.pms` | Keys 1/9 | Key 9 LN and mines on keys 1/9; no scratch |
| `10key.bms`, `14key.bms` | Left/right chord | Two long scratches and left-key/right-scratch mines |

Both fixtures use `#RANK 2` and `#TOTAL 300`. Key 7 forces 7KEY detection
and is outside the input comparison. For manual playback, place any short
PCM WAV named `silent.wav` alongside the charts. The Java test uses a no-op
audio driver.

The fixed sequences in `LunaticRave2ComparisonTest` inject a key-1 tap
(press and release at the specified song time) or advance the clock without
input. Tests check new judgments, a consumed-note bit mask (bit 0 is the
first key-1 note), and current combo after each tick. Multiple judgments
within one tick are reported in judgment-category order, not occurrence order.
Gauge cases independently feed the same scripted judgments to the gauge
logic from a seeded internal percentage. This separates gauge differences
from upstream judgment differences. Gauge checks do not drive stage transitions.

The **beatoraja** and **LunaticRave2** columns execute the production judge
and gauge. Both are asserted separately. The **OpenLR2** column contains
hand-derived expectations from the `v261101` sources, not output from
running OpenLR2 or original Lunatic Rave 2.

For manual testing, set `BMSPlayerRule.SINGLE_SONG_RULE` to
`LunaticRave2` and rebuild. Only normal 5/7/9/10/14KEY BMS/PMS single-song play with
NORMAL, EASY or HARD is eligible. Ordinary LN and long scratch are
supported when interpreted as LN (not CN or HCN). BMS mines are supported. bmson,
courses, Practice, autoplay and replays keep the standard rule. The
prototype uses chart RANK rather than custom judge windows, disables
gauge auto-shift for that run, and does not save scores or replays or
submit to IR. The default source setting still selects the existing beatoraja
rule for each mode. Mode-converted charts (such as SP-to-DP) are excluded.

- [Timing and damage setup](https://github.com/GOMazk/OpenLR2/blob/v261101/LR2/LR2_bmsload.cpp#L3614-L3703):
  at rank 2, the ordinary key windows are 18/40/100/200 ms. For these small
  charts, the HARD damage multiplier reaches 10.
- [Single-note processing](https://github.com/GOMazk/OpenLR2/blob/v261101/LR2/Scene04_Play.cpp#L692-L857):
  hits use the truncated note time, but misses use the fractional time.
  Early empty POOR has a strict 1000 ms cutoff. In the dense case, a BAD on
  the first note is followed by a PG on the second from the same press.
- [Gauge update](https://github.com/GOMazk/OpenLR2/blob/v261101/LR2/Scene04_Play.cpp#L149-L168)
  and [stage-fail check](https://github.com/GOMazk/OpenLR2/blob/v261101/LR2/Scene04_Play.cpp#L1488):
  NORMAL/EASY have a 2% floor and no low-HP damage reduction in single play.
  HARD damage is scaled by 0.6 when the starting HP rounded down to a 2%
  step is at most 30%. HARD fails below 2%; the scripted follow-up PG below
  that boundary probes the gauge's no-revival behavior, not gameplay after failure.
  Beatoraja's [stage-fail check](../../../../src/bms/player/beatoraja/play/BMSPlayer.java)
  uses zero with gauge auto-shift off.

Run `./gradlew regressionLunaticRave2LongNoteComparisonTest` for fixed
press, release and direction-switch sequences on `long-notes.bms`.
The [OpenLR2 LN processor](https://github.com/GOMazk/OpenLR2/blob/v261101/LR2/Scene04_Play.cpp#L863-L1053)
stores the head judgment without scoring it. Holding past the truncated
end millisecond, or releasing no more than one GOOD window before it,
scores that head judgment. Releasing earlier scores BAD immediately.
[OpenLR2 merges both scratch directions](https://github.com/GOMazk/OpenLR2/blob/main/LR2/En_input.cpp#L420-L438)
into one lane input, so reverse is not required. In this prototype a
same-timestamp handoff is continuous regardless of FIFO ordering; a
timestamped gap is a release. The tests check both ordering cases,
end timing, and that score and gauge are updated only once per LN.

Run `./gradlew regressionLunaticRave2MineComparisonTest` for mine timing,
including held keys, release, scratch handoff and late FIFO delivery.
[OpenLR2's mine timing](https://github.com/GOMazk/OpenLR2/blob/v261101/LR2/Scene04_Play.cpp#L699-L718)
allows a fresh press strictly inside the early GREAT window; a held lane
triggers only after the mine's chart time. Each hit is represented as one
POOR in beatoraja's non-consuming counter, without changing combo or EX score.
[Mine damage](https://github.com/GOMazk/OpenLR2/blob/v261101/LR2/Scene04_Play.cpp#L235-L260)
is the raw BMS value (`0A` is 10 percentage points), without HARD's
low-HP correction. NORMAL/EASY bottom out at 2%; HARD bottoms out at 0%
and fails below 2%.

Run `./gradlew regressionLunaticRave2ModeComparisonTest` for the new
5/9/10/14KEY fixtures. OpenLR2 uses the same
[RANK windows](https://github.com/GOMazk/OpenLR2/blob/v261101/LR2/LR2_bmsload.cpp#L3610-L3637)
for these modes. Its [DP input dispatch](https://github.com/GOMazk/OpenLR2/blob/v261101/LR2/Scene04_Play.cpp#L1211-L1217)
sends both sides to the same player outside battle, and its
[note counting](https://github.com/GOMazk/OpenLR2/blob/v261101/LR2/LR2_bmsload.cpp#L3321-L3344)
uses the same player index for both sides. The test checks independent
left/right holds, shared score/combo and TOTAL recovery, and damage from
a right-side mine alone or two simultaneous mines on the shared HARD gauge.
Right-side mine routing uses beatoraja's decoded chart lanes; this does not
attempt to reproduce OpenLR2's chart-loader quirks.

Run `./gradlew regressionLunaticRave2DpOrderTest` for `dp-order.bms`.
The prototype groups input by chart millisecond and uses the
[OpenLR2 lane traversal](https://github.com/GOMazk/OpenLR2/blob/v261101/LR2/Scene04_Play.cpp#L1211-L1217):
left before right, scratch before keys on each side. OpenLR2 actually polls
input; grouping FIFO events by millisecond is beatoraja's deterministic
approximation. At 100% NORMAL, a left scratch mine and left BAD are applied
before right PG, which restores the gauge to 100%. For HARD, a PG in the
same pass still counts but cannot revive a gauge below 2%; a PG in a later
pass is not judged after failure. The game checks the settled judgment
state before advancing to normal song completion.

Original LR2 measurements should be recorded separately before treating the
reference column as a strict compatibility target.
