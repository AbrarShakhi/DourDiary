# Privacy Policy for Dour Diary

**Last updated: 19 September 2026**

Dour Diary is a GPS running tracker for Android. This policy explains exactly what the app
records, where it is kept, and what leaves your phone.

## Summary

- Your runs — routes, times, distances and paces — are stored **only on your phone**.
- There is **no account, no sign-in, and no server** belonging to Dour Diary. Your runs are never
  uploaded to us, because there is nowhere to upload them to.
- There is **no advertising, no analytics and no crash reporting**. No third party receives your
  activity.
- The **only** network request the app makes is for map images, which necessarily tells the map
  provider your IP address and roughly which area of the map you are looking at.
- Deleting a run deletes it. Uninstalling the app deletes everything.

## What the app records

**Precise location.** While a run is recording, Dour Diary reads your device's GPS position about
once per second and stores it as the route of that run, together with the times, distances and
paces derived from it.

Location is read **only while a run is recording.** It is not read in the background, when no run
is in progress, or after a run is finished.

**Your settings.** Your chosen theme, units, and audio cue preferences are stored on the device.

That is the complete list. The app does not read your contacts, accounts, photos, files,
microphone, camera, calendar, installed applications, phone number, or advertising identifier.

## Where it is stored

All run data is written to the app's private storage on your device, in an area other
applications cannot read.

Automatic backup is **switched off**. Your runs are not copied to Google Drive, to any cloud
backup, or to a new phone during device-to-device transfer. This is a deliberate choice: a
recorded route is a detailed record of where you have been, and it stays on the device that
recorded it.

## What leaves your device

**Map images only.**

To draw a map, the app requests map tiles from **OpenFreeMap** (`tiles.openfreemap.org`), which
serves data from OpenStreetMap. Like any request to any website, this necessarily discloses to
that server:

- your device's IP address, and
- which part of the map is being displayed, which indicates approximately where you are or where
  the run you are viewing took place.

This is inherent to using an online map, and applies equally to any other map provider. It is why
the app requests the internet permission.

Dour Diary sends **no run data** with these requests. Your routes, times, paces and history are
not transmitted, to OpenFreeMap or to anyone else.

OpenFreeMap operates independently of Dour Diary and under its own terms. If you prefer that no
map request is made at all, avoid the Run tab and use the app without viewing maps; recording
itself works entirely offline and needs no connection.

## Permissions, and why each is needed

| Permission | Why |
|---|---|
| Precise location (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) | To measure the distance, pace and route of a run. Approximate location alone would be wrong by whole city blocks. |
| Foreground service, location foreground service | To keep recording while the screen is off, with a visible notification for as long as it runs. |
| Notifications (`POST_NOTIFICATIONS`) | To show that ongoing recording notification, which is also how a run is paused or finished without unlocking the phone. |
| Vibration (`VIBRATE`) | To signal each kilometre or mile, so a cue is felt when headphones are out. |
| Internet, network state | To fetch map tiles. Nothing else. |

Dour Diary does **not** request background location access. A run is recorded by a foreground
service that you start, which covers the whole run including with the screen off.

## Keeping and deleting your data

Runs are kept until you delete them.

- **Delete one run:** open it from Home or History and choose *Delete run*. The run, its full
  route, and its cached map image are removed.
- **Delete everything:** uninstall the app, or use Android's *Clear storage* for Dour Diary in
  system settings. Either removes all runs and settings permanently.

Because nothing is uploaded, there is no copy elsewhere for us to delete, and no request you need
to send us to have data erased.

## Children

Dour Diary is not directed at children and does not knowingly collect information from them. It
has no account system, no social features and no messaging.

## Security

Run data is held in the app's private storage, which Android isolates from other applications. On
a device that has been rooted or modified, that isolation may not hold. The app does not transmit
your runs, so they are not exposed in transit.

## Changes to this policy

If the app ever collects or transmits anything beyond what is described here, this policy will be
updated before that change ships, and the date at the top will change with it.

## Contact

Questions about this policy or about the app:

**<!-- Replace with a contact email address before publishing -->**

---

*Dour Diary is open source under the MIT License. The source can be inspected to verify every
statement above.*
