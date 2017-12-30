# BleGattCoroutines
**Make Gatt Great Again!** This library allows easy and safer usage of BluetoothGatt in Android.

It does so by taking advantage of the excellent coroutines feature in the Kotlin programming 
language that allows to write asynchronous code in a sequential/synchronous style, which means, without
the callback hell, and without blocking any thread (which would waste memory and decrease performances).

This library makes it possible to have readable and debuggable code that interacts with
Bluetooth Low Energy GATT (General Attribute), that is, the connection part of the Bluetooth Low Energy standard.

## Why this library ?
As we needed to have an Android app interact with a Bluetooth Low Energy device, we found the
Android BluetoothGatt API and a few RxJava libraries built on top of it. Unfortunately, none suited our needs:
- The vanilla Android BluetoothGatt API is extremely hard to get right, because you have to provide a single
instance of what we call a "God Callback", that is, an instance of a class with overrideable methods that
asynchronously receive the results off all operations of a given type (for example, the result of a
characteristic you requested to read, or the result of whether a characteristic write went well or failed.
You're on your own to make readable code with this, where unrelated characteristic reads, writes or other
operation types are dispatched in the same callback method (like `onCharacteristicRead(…)`).
- The RxJava libraries would mean we'd have to learn RxJava, which is known to have a steep learning curve,
steeper than learning another programming language like Kotlin from Java experience, and steeper than learning
Kotlin coroutines plus
[understanding the kotlinx.coroutines library guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md).
Also, RxJava is a big library, even bigger if you have to use both [version 1](http://www.methodscount.com/?lib=io.reactivex%3Arxjava%3A%2B) and [version 2](http://www.methodscount.com/?lib=io.reactivex.rxjava2%3Arxjava%3A%2B) in the same project.
In fact, RxJava2 methods count is higher than the sum of [Kotlin's stdlib and kotlinx.coroutines](http://www.methodscount.com/?lib=org.jetbrains.kotlinx%3Akotlinx-coroutines-android%3A0.20).

## Experimental status

_This library is based on the coroutines feature of Kotlin, as well as the `kotlinx.coroutines` library,
which are both under the experimental status. Consequently, this library inherits this experimental status. Also,
we are expecting to make a few API changes based on your feedback and real world usages to improve this library._

**Since the API design it not final at the moment, we're very open to feedback while you're using this library.**

_Please, open an issue if something can be improved.
If you just want to tell the author what you're doing with this library, feel free to reach out via [Twitter](https://twitter.com/Louis_CAD) DM, or public tweet._

## Usage

As usual, scan BLE devices using `BluetoothLeScanner` to find you `BluetoothDevice`, or create an instance from the MAC address of your target device.

With this `BluetoothDevice` instance, you can create a `GattConnection` object, which will be key to perform Bluetooth GATT operations using coroutines.

On this `GattConnection` object, call `connect()` to initiate the connection attempt.
If you want your code to suspend until the connection is established, call `await()` on the `connect()` result.
Immediately after calling `connect()`, you can perform the operations you want. If you didn't await the connection before attempting your operations, they will just suspend until the connection is established and then will be executed in a first-come first-served basis (just make sure `connect()` is called before any operation, or asynchronously to avoid dead coroutines).

The currently supported GATT operations on the `GattConnection` class are:
- Services discovery, using `disoverServices()` which returns and cache the list of the services on the connected device.
- Characteristic read, using `readCharacteristic(…)`. Services disovery has to be completed before, as usual.
- Characteristic write, using `writeCharacteristic(…)`. Services disovery has to be completed before, as usual.
- ReliableWrite, with `reliableWrite { … }`. Implemented, but couldn't be tested yet. Open an issue if your device supports it
- Descriptor read, using `readDescriptor(…)`. Services disovery has to be completed before, as usual.
- Descriptor write, using `writeDescriptor(…)`. Services disovery has to be completed before, as usual.
- RSSI read, using `readRemoteRssi(…)`.
- NOTIFY characteristics with the `notifyChannel`. These haven't been tested yet. Feedback wanted.
- PHY, using `readPhy()`. Only supported in Android O. Hasn't been tested. We don't know what this is either, to be honest.

When you're done with the BLE device (you need to be done before the device's battery runs out, unless you're dealing with an always on wearable that the user didn't disconnect), call `close()`.

If you want to reconnect within seconds, or a few minutes to the same device, you can call `disconnect()` instead, which will allow to call `connect()` again later. Note that just like `connect()`, `disconnect()` returns a `Deferred<Unit>` which you can await is needed by calling the obvious `await()` fun.

## Examples

Here's a basic example that just logs the characteristics (using [the `print()` method defined here](https://github.com/Beepiz/BleGattCoroutines/blob/e033fdeb82738bc490fa85968ad1ebc8482d2219/app/src/main/java/com/beepiz/blegattcoroutines/sample/extensions/GattPrint.kt#L12)):
```kotlin
fun BluetoothDevice.logGattServices(tag: String = "BleGattCoroutines") = launch(UI) {
    val deviceConnection = GattConnection(bluetoothDevice = this@logGattServices)
    deviceConnection.connect().await() // Await is optional
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
    deviceConnection.disconnect().await() // Disconnection is optional. Useful if you don't close and reconnect later.
    deviceConnection.close() // Close when no longer used it NOT optional 
}
```

The snippet below is the example you can find in the sample, powered by two extension methods for brevity (`deviceFor(…)` and `useBasic { device, services -> … }`. It also uses the `GenericAccess` object, which is the defition of the standard Bluetooth GATT "Generic access". It includes extension functions and properties for easy and readable usage. You can write a similar specification for any BLE device or `BluetoothGattService` you want.
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
This library is not published on jcenter yet (should be done in January of 2018), but it's made of only [4 Kotlin files](https://github.com/Beepiz/BleGattCoroutines/tree/e8e97a390027c59617411a74a7274d186d1b7c54/blegattcoroutines/src/main/java/com/beepiz/bluetooth/gattcoroutines/experimental), so you can already try it out in your project pretty easily.
