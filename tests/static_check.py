#!/usr/bin/env python3
"""Static archive and invariants check. NOT a Groovy compile or Hubitat runtime test."""
from pathlib import Path
import hashlib
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
CODE = ROOT / "src/FOXWARE-FoxyFan.groovy"
RELEASE = ROOT / "Releases/v3.0.4/FOXWARE-FoxyFan-v3.0.4.groovy"
BASELINE = ROOT / "reference/FOXWARE-FoxyFan-v2.0.3-B20.webcore"
MANIFEST = ROOT / "Releases/v3.0.4/SHA256SUMS"
errors = []

def assert_true(test, message):
    if not test:
        errors.append(message)

for file in [CODE, RELEASE, BASELINE, MANIFEST]:
    assert_true(file.is_file(), f"Missing required file: {file}")
if not CODE.is_file() or not BASELINE.is_file():
    print("FAIL: source or reference missing")
    sys.exit(1)

code = CODE.read_text(encoding="utf-8")
base = BASELINE.read_text(encoding="utf-8")
assert_true(RELEASE.read_bytes() == CODE.read_bytes() if RELEASE.exists() else False,
            "Release source does not match development source")
for token in [
    "singleInstance: false", "singleThreaded: true",
    'label title: "Name this FoxyFan instance (e.g., Master Bathroom)", required: false',
    'section("Instance naming")',
    'title: "Enable Fan Control (can be disabled for testing)"',
    'Notification identity: ${notificationIdentity()}',
    'private String notificationIdentity()',
    'String prefix = notificationIdentity()',
    'destination.deviceNotification("${prefix}: ${text}")',
    'input name: "enableControl"',
    'type: "capability.relativeHumidityMeasurement"',
    'type: "capability.switch"', 'type: "capability.notification"',
    'type: "mode"',
    'defaultValue: 6', 'defaultValue: 5', 'defaultValue: 15',
    'defaultValue: 9', 'defaultValue: 80', 'defaultValue: 20',
    'defaultValue: false, required: true',
    'subscribe(humiditySensor, "humidity", humidityChanged)',
    'subscribe(fanSwitch, "switch", fanSwitchChanged)',
    'runEvery1Minute("minuteTick")',
    'runIn(seconds, "manualTimeoutHandler"',
    'state.ffGeneration', 'state.ffAwaitingOff',
    'state.ffShowerBase', 'state.ffDrySinceMs',
    'fanSwitch.on()', 'fanSwitch.off()',
    'postOffRefresh', 'runIn(3, "refreshFanAfterOff"',
    'postOnRefresh', 'refreshFanAfterOn', 'verifyFanAfterOn',
    'state.ffAwaitingOn', 'state.ffOnRequestedAtMs',
    'notifyManualStart', 'notifyManual', 'notifyManualEarlyOff',
    'sendNotice("notifyManualStart"', 'sendNotice("notifyManualEarlyOff"',
    'fanSwitch.hasCommand("refresh")', 'fanSwitch.refresh()',
]:
    assert_true(token in code, f"Missing native invariant: {token}")
for token in [
    "Build      : 20", "11/23/2026, 2:08:05 PM", "{currentRH - rh6m}",
    "greater than or equal to 6", "{showerBase + 9}",
    "addMinutes(fanStartedAt, 15)", "addMinutes(dryStartedAt, 5)",
    "addMinutes(fanStartedAt, 80)", "addMinutes(manualFanStartedAt, 20)",
]:
    assert_true(token in base, f"Missing baseline invariant: {token}")
assert_true('FOXWARE FoxyFan (${app.label' not in code,
            'Old duplicated notification prefix must not be present')
assert_true(code.count('deviceNotification(') == 1,
            'Notifications must share a single deviceNotification send path')
assert_true(base.count("Send PUSH notification") == 3, "Reference B20 should have 3 PUSH calls")
assert_true("@humidtrip" not in code, "Native app must not depend on global @humidtrip")
assert_true('no live Hubitat validation has been performed' not in code,
            'Stale untested-draft app UI text still present')
assert_true('sendNotice("notifyManual"' in code, 'Existing manual timeout notification lost')
assert_true('endRun("DRY"' in code and 'endRun("MAXIMUM"' in code and 'endRun("MANUAL"' in code,
            'All three OFF paths must converge on endRun')
assert_true('fanSwitch.off()' in code and code.count('fanSwitch.off()') == 1,
            'OFF commands should only occur in the shared endRun path')

# Simple bracket balancing (skips Groovy // and /* */ comments and quoted strings).
# This does NOT validate Groovy grammar or Hubitat-specific APIs.
def brackets_balanced(s):
    stack = []
    pairs = {')': '(', ']': '[', '}': '{'}
    i = 0
    line = 1
    while i < len(s):
        c = s[i]
        if c == '\n':
            line += 1
            i += 1
            continue
        if s.startswith("//", i):
            j = s.find("\n", i)
            i = len(s) if j < 0 else j
            continue
        if s.startswith("/*", i):
            j = s.find("*/", i + 2)
            if j < 0:
                return f"Unclosed comment at line {line}"
            line += s[i:j+2].count("\n")
            i = j + 2
            continue
        if c in ['\"', "'"]:
            quote = c
            i += 1
            while i < len(s):
                if s[i] == '\\':
                    i += 2
                    continue
                if s[i] == quote:
                    i += 1
                    break
                if s[i] == '\n':
                    line += 1
                i += 1
            else:
                return f"Unclosed string at line {line}"
            continue
        if c in "([{":
            stack.append((c, line))
        elif c in ")]}":
            if not stack or stack[-1][0] != pairs[c]:
                return f"Unmatched {c} at line {line}"
            stack.pop()
        i += 1
    if stack:
        return f"Unclosed bracket {stack[-1]}"
    return None

balance_error = brackets_balanced(code)
assert_true(balance_error is None, balance_error or "")

if MANIFEST.exists():
    for line in MANIFEST.read_text().splitlines():
        if not line.strip():
            continue
        parts = re.match(r"^([0-9a-f]{64})  (.+)$", line)
        if not parts:
            errors.append(f"Malformed manifest line: {line}")
            continue
        target = MANIFEST.parent / parts.group(2)
        assert_true(target.exists(), f"Manifest target missing: {target}")
        if target.exists():
            digest = hashlib.sha256(target.read_bytes()).hexdigest()
            assert_true(parts.group(1) == digest, f"Digest mismatch: {target.name}")

if errors:
    print("FAIL —", len(errors), "problem(s)")
    for e in errors:
        print(" -", e)
    sys.exit(1)
print("PASS — v3.0.4 source/release parity; verified Build 20 baseline tokens; native design invariants; bracket balance; SHA256SUMS")
print("NOTE — static checks only; Hubitat Groovy compile and live device tests NOT performed")
