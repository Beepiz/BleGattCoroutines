# Change log for BleGattCoroutines

## Version 0.4.1 (2019-05-08)

Version 0.4.0 publication was broken because gradle metadata was not enabled, and any
usage would lead to gradle sync and gradle build fails on consumer projects.

This has now been fixed. Sorry for the inconvenience.

## Version 0.4.0 (2019-05-05)
**Publication of this version is broken, use 0.4.1 or newer instead.**
Compiled with Kotlin 1.3.31 (and kotlinx.coroutines 1.2.1).

This is an important release for BleGattCoroutines that includes fixes, improvements & new features.

### Fixes
- Fix rare hard to track uncaught exceptions caused by `offer` called on closed channels. (744e862f)
- Automatically request disconnect on close by default. Avoids issue where connection is not stopped by close on Android's side and prevents subsequent connections.

### New features
- Add a public `bluetoothDevice` property to the `GattConnection` interface, thanks to [Brian Parma](https://github.com/bj0) contribution.
- Add `requestMtu` function on `GattConnection`, thanks to [Bill Cauchois](https://github.com/wcauchois) contribution.
- Add first-class notifications enabling on remote device with `setCharacteristicNotificationsEnabledOnRemoteDevice`. This was the most requested feature.
- Add `openNotificationSubscription` function to receive notifications only for a specific characteristic. This can replace usage of the `notifyChannel` property.

### Changes
- The `notifyChannel` property now returns a new subscription each time, so it is possible to have multiple consumers.
- Exceptions to catch are now mentioned in `GattConnection` KDoc. (b85c9bea)
- Add workaround for `Dispatchers.Main` initialization doing blocking I/O (8783db98).
- Drop main thread usage requirement. You can now use the dispatcher you want.
- Samples and `@RequiresApi` annotations migrated to AndroidX.
- Samples now properly request runtime permissions, with a single suspending function call thanks to [Splitties permissions](https://github.com/LouisCAD/Splitties/tree/master/modules/permissions).
- Minor doc improvements.

## Version 0.3.0 (2018-11-27)

### Kotlin 1.3
This release is compiled with Kotlin 1.3.10 (and kotlinx.coroutines 1.0.1), which
means it is now using stable coroutines. The [Splitties](https://github.com/LouisCAD/Splitties)
artifacts have also been updated to [version 2.0.0](
https://github.com/LouisCAD/Splitties/blob/e77c909585f1b6d457af0fe18655e4794434ce50/CHANGELOG.md#version-200-2018-11-13
).

### Changes
- Now uses stable coroutines.
- All the package names changed, dropping `experimental` from the hierarchy.
- The API is still experimental, and is annotated accordingly.


## Version 0.2.0 (2018-11-27)
This release is compiled with Kotlin 1.2.71 and relies on the version 0.30.2
of kotlinx.coroutines.

It also relies on three [Splitties](https://github.com/LouisCAD/Splitties)
artifacts (App Context, Main Thread & Checked Lazy) of the [version 2.0.0-alpha06](
https://github.com/LouisCAD/Splitties/blob/e77c909585f1b6d457af0fe18655e4794434ce50/CHANGELOG.md#version-200-alpha6-2018-11-11
).

### Changes
- `GattConnection` is now an interface (was previously a class). You can still instantiate it
with constructor-like syntax.
- Instead of suspending any pending request when a disconnection has occured, a
`ConnectionClosedException` will be thrown. _Note that it is a subclass of `CancellationException`,
so it will cancel the coroutine without crashing your application, but you should still handle it
properly, especially if you want to retry or recover._
- You can pass `ConnectionSettings` when creating a `GattConnection` instance. With this, you can
change transport, physical layer (aka. PHY) and enable auto connect.
- `requireXxx` extension functions for `GattConnection`, `BluetoothGattService` and
`BluetoothGattCharacteristic` thanks to [Mike (@Miha-x64)](https://github.com/Miha-x64) contribution.


## Version 0.1.0 (2018-03-19)
This is the first release of BleGattCoroutines.
Said otherwise, this was the start of the **Make Gatt Great Again!** campaign.

This version targets experimental coroutines from Kotlin 1.2.30 and
relies on the version 0.22.5 of kotlinx.coroutines.
Its target Android SDK is API level 27.
