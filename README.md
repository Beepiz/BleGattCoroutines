# BleGattCoroutines
**Make Gatt Great Again!** This library allows easy and safer usage of
BluetoothGatt in Android. It has also been tested successfully on
**Android Wear**, with the sample included in this repository. It should
work similarly on other Android variants such as Android Things.

It does so by taking advantage of the excellent coroutines feature in the
Kotlin programming language that allows to write asynchronous code in a
sequential/synchronous style, which means, without the callback hell, and
without blocking any thread (which would waste memory and decrease
performances).

This library makes it possible to have readable and debuggable code that
interacts with Bluetooth Low Energy GATT (General Attribute), that is, the
connection part of the Bluetooth Low Energy standard.

## Why this library ?
As we needed to have an Android app interact with a Bluetooth Low Energy
device, we found the Android BluetoothGatt API and a few RxJava libraries
built on top of it. Unfortunately, none suited our needs:
- The vanilla Android BluetoothGatt API is extremely hard to get right,
because you have to provide a single instance of what we call a "God
Callback", that is, an instance of a class with overridable methods that
asynchronously receive the results off all operations of a given type (for
example, the result of a characteristic you requested to read, or the result
of whether a characteristic write went well or failed. You're on your own to
make readable code with this, where unrelated characteristic reads, writes
or other operation types are dispatched in the same callback method (like
`onCharacteristicRead(…)`).
- The RxJava libraries would mean we'd have to learn RxJava, which is known
to have a steep learning curve, steeper than learning another programming
language like Kotlin from Java experience, and steeper than learning Kotlin
coroutines plus [understanding the kotlinx.coroutines library guide](
https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md
). Also, RxJava is a big library, even bigger if you have to use both
[version 1](http://www.methodscount.com/?lib=io.reactivex%3Arxjava%3A%2B)
and [version 2](
http://www.methodscount.com/?lib=io.reactivex.rxjava2%3Arxjava%3A%2B) in the
same project. In fact, RxJava2 methods count is higher than the sum of
[Kotlin's stdlib and kotlinx.coroutines](
http://www.methodscount.com/?lib=org.jetbrains.kotlinx%3Akotlinx-coroutines-android%3A0.20
).

## Experimental status

_This library is based on the coroutines feature of Kotlin, as well as the
`kotlinx.coroutines` library, which are both under the experimental status.
Consequently, this library inherits this experimental status. Also, we are
expecting to make a few API changes based on your feedback and real world
usages to improve this library._

**Since the API design it not final at the moment, we're very open to
feedback while you're using this library.**

_Please, open an issue if something can be improved. If you just want to
tell the author what you're doing with this library, feel free to reach out
via [Twitter](https://twitter.com/Louis_CAD) DM, or public tweet._

## Usage

As usual, scan BLE devices using `BluetoothLeScanner` to find you
`BluetoothDevice`, or create an instance from the MAC address of your target
device.

With this `BluetoothDevice` instance, you can create a `GattConnection`
object, which will be key to perform Bluetooth GATT operations using
coroutines.

On this `GattConnection` object, call `connect()` to initiate the connection.  Immediately after
It will suspend until the connection is established, then you use the
connection:

The currently supported GATT operations on the `GattConnection` class are:
- Services discovery, using `discoverServices()` which returns and cache
the list of the services on the connected device.
- Characteristic read, using `readCharacteristic(…)`. Services discovery has
to be completed before, as usual.
- Characteristic write, using `writeCharacteristic(…)`. Services discovery
has to be completed before, as usual.
- ReliableWrite, with `reliableWrite { … }`. Implemented, but couldn't be
tested yet. Open an issue if your device supports it
- Descriptor read, using `readDescriptor(…)`. Services discovery has to be
completed before, as usual.
- Descriptor write, using `writeDescriptor(…)`. Services discovery has to be
completed before, as usual.
- RSSI read, using `readRemoteRssi(…)`.
- NOTIFY characteristics with the `notifyChannel`. These haven't been tested
yet. Feedback wanted.
- Toggling characteristic update notifications with
`setCharacteristicNotificationsEnabled(…)`. Tied to NOTIFY feature.
- PHY, using `readPhy()`. Only supported in Android O. Hasn't been tested.
We don't know what this is either, to be honest.

When you're done with the BLE device (you need to be done before the
device's battery runs out, unless you're dealing with an always on wearable
that the user didn't disconnect), call `close()`.

If you want to reconnect within seconds, or a few minutes to the same device,
you can call `disconnect()` instead, which will allow to call `connect()`
again later.

## Examples

Here's a basic example that just logs the characteristics (using [the `print()` method defined here](https://github.com/Beepiz/BleGattCoroutines/blob/e033fdeb82738bc490fa85968ad1ebc8482d2219/app/src/main/java/com/beepiz/blegattcoroutines/sample/extensions/GattPrint.kt#L12)):
```kotlin
fun BluetoothDevice.logGattServices(tag: String = "BleGattCoroutines") = launch(UI) {
    val deviceConnection = GattConnection(bluetoothDevice = this@logGattServices)
    deviceConnection.connect() // Suspends until connection is established
    val gattServices = deviceConnection.discoverServices() // Suspends until completed
    gattServices.forEach {
        it.characteristics.forEach {
            try { 
                deviceConnection.readCharacteristic(it) // Suspends until characteristic is read
            } catch (e: Exception) {
                Log.e(tag, "Couldn't read characteristic with uuid: ${it.uuid}", e)
            }
        }
        Log.v(tag, it.print(printCharacteristics = true))
    }
    deviceConnection.disconnect() // Disconnection is optional. Useful if you don't close and reconnect later.
    deviceConnection.close() // Close when no longer used it NOT optional 
}
```

The snippet below is the example you can find in the sample, powered by two extension methods for brevity (`deviceFor(…)` and `useBasic { device, services -> … }`). It also uses the `GenericAccess` object, which is the definition of the standard Bluetooth GATT "Generic access". It includes extension functions and properties for easy and readable usage. You can write a similar specification for any BLE device or `BluetoothGattService` you want.
```kotlin
private val myEddystoneUrlBeaconMacAddress = "F2:D6:43:93:70:7A"
private val defaultDeviceMacAddress = myEddystoneUrlBeaconMacAddress

fun logNameAndAppearance(deviceMacAddress: String = defaultDeviceMacAddress) = launch(UI) {
    deviceFor(deviceMacAddress).useBasic { device, services ->
        services.forEach { Timber.d("Service found with UUID: ${it.uuid}") }
        with(GenericAccess) {
            device.readAppearance()
            Timber.d("Device appearance: ${device.appearance}")
            device.readDeviceName()
            Timber.d("Device name: ${device.deviceName}")
        }
    }
}
```
When connected to my [Google Beacon](https://twitter.com/GDGTours/status/732992233817972736), the code above outputs the following in logcat:
```console
I/MainViewModel$logNameAndAppearance: Connected!
I/MainViewModel$logNameAndAppearance: Services discovered!
D/MainViewModel$logNameAndAppearance: Service found with UUID: 00001800-0000-1000-8000-00805f9b34fb
D/MainViewModel$logNameAndAppearance: Service found with UUID: 00001801-0000-1000-8000-00805f9b34fb
D/MainViewModel$logNameAndAppearance: Service found with UUID: ee0c2080-8786-40ba-ab96-99b91ac981d8
D/MainViewModel$logNameAndAppearance: Device appearance: 512
D/MainViewModel$logNameAndAppearance: Device name: eddystone Config
I/MainViewModel$logNameAndAppearance: Disconnected!
I/MainViewModel$logNameAndAppearance: Closed!
```
This proves our library is working and that **WE MADE GATT GREAT AGAIN!**

## Download

### Gradle instructions
Make sure you have `jcenter()` in the repositories defined in your project's
(root) `build.gradle` file (default for new Android Studio projects).

Add the version of the library to not repeat yourself if you use multiple
artifacts, and make sure their versions are in sync by adding an ext property
into your root project `build.gradle` file:
```groovy
allProjects {
    ext {
        blegattcoroutines_version = '0.1.0'
    }
}
```
Here are all the artifacts of this library. Just use the ones you need:
```groovy
implementation "com.beepiz.blegattcoroutines:blegattcoroutines-core:$blegattcoroutines_version"
implementation "com.beepiz.blegattcoroutines:blegattcoroutines-genericaccess:$blegattcoroutines_version"
```
#### Snapshots
Let's say you need a new feature or a fix that did
not make it to a release yet:

You can grab it in the latest snapshot by adding the
snapshots repository and changing the library version to the -SNAPSHOT
version in your root project `build.gradle` file:

```groovy
allProjects {
    repositories {
        google()
        jcenter() // Add snapshots repo below
        maven { url 'https://oss.jfrog.org/artifactory/oss-snapshot-local' }
    }
    ext {
        blegattcoroutines_version = '0.1.0-SNAPSHOT' // Change this line
    }
}
```

If you need to, you can browse the deployed snapshots [here on artifactory](
https://oss.jfrog.org/webapp/#/artifacts/browse/tree/General/oss-snapshot-local/com/beepiz/blegattcoroutines
).

### Other build systems
For maven and alternative build-systems, check the [Bintray page](
https://bintray.com/beepiz/maven/blegattcoroutines).

## New versions notifications
To get notified for new versions, be sure to click on "Watch" on the
[BleGattCoroutines Bintray page](https://bintray.com/beepiz/maven/blegattcoroutines).
