# Motion Alert

A minimal motion detector for a bike. Nothing else: no GPS tracking, no
guard, no SOS, no map.

## How it works

The same app runs on two phones. First launch asks which phone this is.

- **Bike phone**: a foreground service listens to the accelerometer. When
  the bike is handled or shaken, it posts a motion event to a small
  Cloudflare Worker (`motion-alerts`). One alert at most every 2 minutes.
  The service restarts after a reboot. A "Send test alert" button sends
  one event on demand.
- **Owner phone**: registers its FCM token with the Worker. When a motion
  event arrives, the phone posts a "Motion detected" notification. Tapping
  it opens Google Find Hub, where the bike can be seen on the map. When
  Find Hub is not installed, its Play Store page opens instead.

The Worker lives in `~/workspace/motion-alerts/worker.js` and is deployed
with `~/workspace/custom-connectors/custom.cloudflare/bin/deploy_motion_alerts.py`.
It reuses the "My First Project" Firebase project (deft-processor-338922)
for FCM. This app never uses GPS itself: Find Hub does the locating.

## Setup notes

- Bike phone needs battery optimization off (the app asks on first launch)
  and Autostart on MIUI, or the service can be killed.
- The bike phone must be signed into a Google account that Find Hub on
  the owner phone can see.
- Motion detection copies BikeGuard's accelerometer rule: a 1.5 m/s²
  deviation from gravity counts as a shake, 4 shakes within 3 seconds
  count as motion.

## Build

GitHub Actions builds the release APK on push to main and on `v*` tags.
Required secrets: `GOOGLE_SERVICES_JSON` (base64 of google-services.json),
`MOTION_SECRET`, `KEYSTORE_B64_A/B/C/D`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`.
