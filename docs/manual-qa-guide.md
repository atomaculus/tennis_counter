# PLAYCE - Manual QA Guide

Last updated: March 27, 2026

This guide is designed for real-device QA before release. Follow it in order. If you find a bug, capture the result using the bug report template at the end and bring it back here for debugging.

## 1. Test setup

Required devices:
- one Android phone with the `:mobile` app installed
- one Wear OS watch with the `:app` app installed
- optional second Wear OS watch for spectator mode

Recommended install state before starting:
- both apps installed from the same build candidate
- phone and watch paired and visible to each other
- Bluetooth enabled
- both devices charged above 30%

Recommended build commands before device testing:

```powershell
.\gradlew.bat :app:assembleDebug :mobile:assembleDebug
.\gradlew.bat :app:assembleDebugAndroidTest
.\gradlew.bat :mobile:testDebugUnitTest
```

## 2. Logging to keep handy

Useful log tags:
- `WearDataLayer`
- `WearAckListener`
- `MatchTimer`
- `MatchTimerService`
- `WearMatchListener`

If a flow fails, try to capture:
- exact device pair used
- exact step number from this guide
- what you expected
- what actually happened
- screenshot or short video if visual
- relevant log lines if available

## 3. Test run format

For each step mark one of:
- `PASS`
- `FAIL`
- `BLOCKED`

Suggested notation:

```text
3.4 PASS
3.5 FAIL - halo did not switch side after score changed to 15-0
4.3 BLOCKED - phone disconnected unexpectedly
```

## 4. Core Wear scoring

### 4.1 Fresh match startup

Steps:
1. Open the Wear app.
2. Start a new match.
3. Observe the initial score and timer.

Expected:
- score starts at `0-0`
- games and sets start at `0-0`
- timer starts automatically
- Player A is highlighted as initial server

### 4.2 Basic point progression

Steps:
1. Add four points to Player A.
2. Start a fresh match again.
3. Add points in this order: A, A, B, A.

Expected:
- labels progress through `0`, `15`, `30`, `40`
- winning the fourth point without deuce closes the game
- score labels always remain readable

### 4.3 Deuce and advantage

Steps:
1. Start a fresh match.
2. Reach `40-40`.
3. Give Player A one point.
4. Give Player B one point.
5. Give Player B one more point.

Expected:
- `40-40` becomes deuce
- next point shows `AD-40` or `40-AD`
- losing the advantage returns to deuce
- winning after advantage closes the game correctly

### 4.4 Undo actions

Steps:
1. During a normal game, add at least two points.
2. Long press Player A side to undo A's last point.
3. Long press Player B side to undo B's last point when applicable.
4. Cross a game boundary, then undo.

Expected:
- undo removes the last applicable point
- undo after a completed game restores the previous game state correctly
- no negative scores or broken labels appear

### 4.5 Admin actions

Steps:
1. Start a match and play into a non-zero game state.
2. Trigger admin actions with the simultaneous long press gesture.
3. Use `reset game`.
4. Play again, then use `reset match`.
5. Finish a match and use `new match`.

Expected:
- `reset game` clears only current game points
- `reset match` clears full match state
- `new match` returns to a fresh playable match

### 4.6 Serve indicators

Steps:
1. Start a new match and note which player is highlighted as server.
2. Within the first game, add one point and observe the serve-side halo.
3. Add another point and observe the halo again.
4. Finish the game and confirm server highlight switches players.
5. Play enough games to begin a new set and confirm serve alternation continues.

Expected:
- server highlight matches the current server
- halo switches left/right with even/odd point count inside the game
- server changes after each game
- server sequence continues across set boundaries
- `reset game` does not incorrectly flip the current server

## 5. Wear timer resilience

### 5.1 Background and resume

Steps:
1. Start a match.
2. Let the timer run for at least 20 seconds.
3. Leave the app in background.
4. Return to the app after 10 to 20 seconds.

Expected:
- elapsed time is preserved
- timer continues from a sensible value

### 5.2 Remove from recents

Steps:
1. Start a match and let time advance.
2. Remove the app from recent tasks.
3. Reopen the app.

Expected:
- previous timer state is consolidated
- timer does not silently reset during an active match
- fresh scoreboards should not inherit stale time

### 5.3 Reboot recovery

Steps:
1. Start a match and let time advance.
2. Reboot the watch.
3. Reopen PLAYCE.

Expected:
- restored timer state is reasonable
- app is not stuck or reset into a broken state

## 6. Wear -> mobile sync

### 6.1 Normal connected save

Steps:
1. Keep phone and watch connected.
2. Finish a match on the watch.
3. Tap `SAVE MATCH`.
4. Watch the sync state on Wear and the history screen on phone.

Expected:
- Wear shows `Syncing...` and then a synced state
- phone inserts exactly one history entry
- saved result matches the final score and set summary

### 6.2 Offline then reconnect

Steps:
1. Disconnect the phone or disable the link so watch cannot reach it.
2. Finish a match on the watch and save it.
3. Reconnect phone and watch.
4. Wait for retry.

Expected:
- Wear keeps a pending state instead of losing the save
- retry happens after reconnect
- phone eventually receives exactly one match

### 6.3 Duplicate protection

Steps:
1. Save a finished match successfully.
2. Repeat the same flow in conditions likely to cause repeated delivery.
3. Check phone history after retries or reconnects.

Expected:
- phone history still contains only one stored match for that result
- no visible duplicate insertions

### 6.4 Premium locked path

Steps:
1. Use a phone state without Premium unlocked.
2. Finish and save a watch match.
3. Inspect mobile history and Wear sync feedback.

Expected:
- mobile does not save the match into history
- sync resolves with a Premium-locked outcome rather than hanging forever

## 7. Mobile UX validation

### 7.1 Live score replacement

Steps:
1. Start a match from the watch.
2. Open the counter tab on the phone while the match is active.

Expected:
- phone shows live read-only score instead of the manual local counter
- score changes quickly as points are added on the watch

### 7.2 History and detail

Steps:
1. Save at least one completed match from the watch with Premium available.
2. Open history.
3. Open match detail.

Expected:
- history card shows correct final score
- set detail appears when available
- detail screen matches the stored result

### 7.3 Share flow

Steps:
1. Open a saved match detail.
2. Generate the share card.
3. Share it to another app.

Expected:
- share card renders correctly
- Android share sheet opens
- target app receives the generated image

### 7.4 Free mobile counter

Steps:
1. Ensure Premium is not unlocked.
2. Open the mobile counter without an active watch match.
3. Use the local counter manually.

Expected:
- free counter remains usable
- premium-only areas remain blocked

## 8. Spectator mode

Run this only if a second watch is available.

Steps:
1. Start a live match on Watch A.
2. Open PLAYCE on Watch B.
3. Enter spectator mode if offered.
4. Add points on Watch A.
5. End the match on Watch A.

Expected:
- Watch B detects active live match
- spectator score updates without local scoring controls
- spectator screen exits or clears correctly when match ends

## 9. Release sanity check

Before considering the build a release candidate, confirm all of these:
- all critical flows above are `PASS`
- no unresolved `FAIL` remains in sync, timer, scoring, or purchases
- `.\gradlew.bat :mobile:bundleRelease :app:assembleRelease` succeeds for the candidate commit
- privacy policy URL is public
- Play Console Data safety matches the shipped build configuration

## 10. Bug report template

Copy this template when you hit an issue:

```text
Title:

Step:

Environment:
- Phone model:
- Android version:
- Watch model:
- Wear OS version:
- Premium state:
- Connected/disconnected:

Expected:

Actual:

Frequency:
- always / intermittent / happened once

Artifacts:
- screenshot:
- video:
- logs:

Notes:
```

## 11. How to bring failures back to Codex

When you report a failure, include:
- the exact step number from this guide
- whether it is reproducible
- whether it blocks release or is cosmetic
- any screenshot, video, or log snippet you captured

That is enough for me or another Codex instance to pick up the issue and work on it without reconstructing the whole context from zero.
