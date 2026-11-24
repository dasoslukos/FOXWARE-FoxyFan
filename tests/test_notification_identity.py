#!/usr/bin/env python3
"""Static v3.0.4 notification-identity contract + illustrative fixtures.

Does not execute Groovy or Hubitat app code. Use TEST-PLAN.md on hub for runtime.
"""
from pathlib import Path

code = (Path(__file__).resolve().parents[1]/'src/FOXWARE-FoxyFan.groovy').read_text()
assert 'private String notificationIdentity()' in code
assert 'section("Instance naming")' in code
assert 'label title: "Name this FoxyFan instance (e.g., Master Bathroom)", required: false' in code
assert 'app.updateLabel' not in code  # built-in label UI manages the installed-app label
assert 'input name: "customName"' not in code
assert 'String installedLabel = app?.label?' in code
assert '!installedLabel.equalsIgnoreCase(brand)' in code
assert 'String fanName = fanSwitch?.displayName?' in code
assert 'return "${brand} (${fanName})"' in code
assert 'return "${brand} (instance ${app.id})"' in code
assert 'String prefix = notificationIdentity()' in code
assert 'destination.deviceNotification("${prefix}: ${text}")' in code
assert 'FOXWARE FoxyFan (${app.label' not in code
assert code.count('deviceNotification(') == 1

# Illustrative model for the documented identity-precedence requirements;
# these do not execute any Groovy runtime paths.
def example_identity(installed_label, fan_name, app_id):
    brand = 'FOXWARE FoxyFan'
    label = ' '.join((installed_label or '').split())
    if label and label.lower() != brand.lower():
        if label.lower().startswith(brand.lower() + ' '):
            return label
        return f'{brand} ({label})'
    fan = ' '.join((fan_name or '').split())
    return f'{brand} ({fan})' if fan else f'{brand} (instance {app_id})'

fixtures = [
    ('FOXWARE FoxyFan', 'Master Bath Fan', 1418, 'FOXWARE FoxyFan (Master Bath Fan)'),
    ('FOXWARE FoxyFan', 'Guest Bath Fan', 1501, 'FOXWARE FoxyFan (Guest Bath Fan)'),
    ('Master Bathroom', 'Master Bath Fan', 1418, 'FOXWARE FoxyFan (Master Bathroom)'),
    ('Guest Bathroom', 'Guest Bath Fan', 1501, 'FOXWARE FoxyFan (Guest Bathroom)'),
    ('FOXWARE FoxyFan - Guest Bath', 'Guest Bath Fan', 1501, 'FOXWARE FoxyFan - Guest Bath'),
    ('FOXWARE FoxyFan', None, 1418, 'FOXWARE FoxyFan (instance 1418)'),
]
for label, fan, app_id, expected in fixtures:
    actual = example_identity(label, fan, app_id)
    assert actual == expected, (label, fan, actual, expected)

print(f'PASS — notification formatter source contract and {len(fixtures)} illustrative identity fixtures')
print('NOTE — source inspection + reference model, NOT Groovy execution or live Hubitat verification')
