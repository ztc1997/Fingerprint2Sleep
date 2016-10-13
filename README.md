[![GitHub version](https://badge.fury.io/gh/ztc1997%2FFingerprint2Sleep.svg)](https://github.com/ztc1997/Fingerprint2Sleep/releases) ![license](http://img.shields.io/badge/license-GPLv3-brightgreen.svg) ![platform](http://img.shields.io/badge/platform-Android-blue.svg)
# Fingerprint Quick Action

Perform quick actions via tap fingerprint sensor.

Many devices have fingerprint sensor, and have the feature fingerprint to unlock or fingerprint to wake, so i developed this simple app to sleep your device via tap fingerprint sensor.

Fingerprint to Unlock use DevicePolicyManager API to sleep your device by default, however, in many ROMs, it will cause Smart Lock and fingerprint to unlock doesn't work, by my test, it is only works properly on MIUI ROMs. So the app provide another way to sleep your device, that is simulating a power button press, this method requires Root Access.

When other apps using the fingerprint sensor, Fingerprint to Unlock will be temporarily disabled until next time you switch on screen.

## Tips

1. If you Boot Manager app, please allow `Fingerprint to Sleep` autostart.<br>
On MIUI, go to `Security`→`Permissions`→`Autostart`, and choose `Fingerprint to Sleep`.
2. If you use Task Management app, please add `Fingerprint to Sleep` to whitelist.<br>
On MIUI, go to `Settings`→`Battery & performance`→`Manager apps battery usage`
→`Choose apps`→`Fingerprint to Sleep`, and choose `No restriction`.

## Donate

If you like this app and want to support the developer of this repo, consider buying me a cup of coffee.<br>
[Donate](./DONATE.md)

## License
```
Copyright 2016 Alex Zhang aka. ztc1997

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```