<!--
SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Privacy Policy — Weather

_Effective 4 November 2026_

Weather (package `org.prauga.weather`) is a weather app published by Saalim Quadri for PVOT-OSS.
This policy explains what the app does with your data. The [Terms of Service](TermsOfService.md) cover
using the app.

**In short: the app has no accounts, no ads, no analytics and no tracking, and it never sends your
data to us. To get forecasts it sends the coordinates of the places you look at to two weather
services, Open-Meteo and MET Norway.**

## What the app sends, and to whom

The app talks only to the services below, directly from your phone. Like any internet request, each
one also reveals your IP address to the service. Every request identifies the app as
`PvotWeather/<version>` with a link to its source code, but carries no account, device ID or
advertising ID.

| Service | What for | What it receives |
|---|---|---|
| [Open-Meteo](https://open-meteo.com/) | Forecasts, place search, air quality and pollen, past temperatures, sea, river flow and ensemble forecasts | The coordinates of the place (rounded to 4 decimals, about 10 m) and its elevation; for place search, the text you type and your device's language |
| [MET Norway](https://api.met.no/) | Forecasts, rain radar, warnings, the moon, air quality and sea forecasts | The coordinates of the place (rounded to 4 decimals) and its elevation; the date and time zone offset for the moon; your language for warnings |

Open-Meteo's and MET Norway's own privacy terms apply to what they receive:
[Open-Meteo terms](https://open-meteo.com/en/terms) and
[MET Norway terms of service](https://api.met.no/doc/TermsOfService).

The app asks for a place's weather while you use it. If you have a home screen widget, it also
refreshes about once an hour in the background, sending the selected place's coordinates to
Open-Meteo only.

## Your location

The app uses your location only if you tap **Use my location** and allow it. You can choose
approximate or precise location, allow it only once, or refuse; you can still add places by
searching.

- Your position comes from Android's location service. On phones with Google Play services this may
  be Google's fused location provider, under the [Google Privacy Policy](https://policies.google.com/privacy).
- To name the place (for example the neighbourhood), the app asks Android's geocoder, which may send
  your coordinates to the geocoding service your phone uses (on most phones, Google's).
- When the "Current location" place is selected, the app updates your position each time it
  refreshes while you are using it.

## What the app keeps on your device

- **Your saved places**, including the "Current location" place with its coordinates.
- **Recent forecasts** from Open-Meteo and MET Norway for your places, so the app works offline and
  doesn't ask the services more often than they allow.
- **Your settings**: units, the selected place and the sources setting.
- **Widget state** used to refresh the widgets and their previews. It contains no personal data.

All of this stays on your phone and is deleted when you clear the app's storage or uninstall it.
Removing a place also deletes the forecasts kept for it.

If you have turned on Android's backup, Android may include this data (saved places, recent forecasts
and settings) in your device backup and in transfers to a new phone, under your backup provider's
terms. The app itself never uploads it.

Home screen widgets show the selected place and its weather on your home screen, where anyone who
can see your screen can read them.

## Permissions

- `INTERNET`: to get forecasts from Open-Meteo and MET Norway, as described above.
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`: to find where you are when you tap
  **Use my location**. The app asks only then; you can refuse.
- `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`: added by
  Android's WorkManager library, which the widgets use to refresh. They don't give the app access to
  any of your data.

## Children

The app is not directed at children and collects no data from anyone, children included.

## Changes

If the app starts handling data differently, this policy will be updated before that version is
released, with a new effective date at the top.

## Contact

Questions about this policy: danascape@gmail.com, or open an issue at
https://github.com/PVOT-OSS/PvotWeather/issues.
