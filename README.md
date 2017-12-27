# BleGattCoroutines
**Make Gatt Great Again!** This library allows easy and safer usage of BluetoothGatt in Android.

It does so by taking advantage of the excellent coroutines feature in the Kotlin programming 
language that allows to write asynchronous code in a sequential/synchronous style, which means, without
the callback hell, and without blocking any thread (which would waste memory and decrease performances).

This library makes it possible to have readable and debuggable code that interacts with
Bluetooth Low Energy GATT (General Attribute), that is, the connection part of the Bluetooth Low Energy standard.

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

## Usage
_Note that this library is based on the coroutines feature of Kotlin, as well as the `kotlinx.coroutines` library,
which are both under the experimental status. Consequently, this libray inherits this experimental status. Also,
we are expecting to make a few API changes based on your feedback and real world usages to improve this library._

Here's a basic example that just logs the characteristics (using [the `print()` method defined here](https://github.com/Beepiz/BleGattCoroutines/blob/e033fdeb82738bc490fa85968ad1ebc8482d2219/app/src/main/java/com/beepiz/blegattcoroutines/sample/extensions/GattPrint.kt#L12)):
```kotlin
fun BluetoothDevice.logGattServices(tag: String = "BleGattCoroutines") = launch(UI) {
    val deviceConnection = GattConnection(bluetoothDevice = this@logGattServices)
    deviceConnection.connect().await() // Await is optional
    val gattServices = deviceConnection.discoverServices() // Suspends until completed
    gattServices.forEach {
        it.characteristics.forEach {
            deviceConnection.readCharacteristic(it) // Suspends until characteristic is read
        }
        Log.v(tag, it.print(printCharacteristics = true))
    }
    deviceConnection.disconnect().await() // Disconnection is optional.
    deviceConnection.close() // Close when no longer used it NOT optional 
}
```

## Download
This library is not published on jcenter yet (should be done in January of 2018), but it's made of only [4 Kotlin files](https://github.com/Beepiz/BleGattCoroutines/tree/e8e97a390027c59617411a74a7274d186d1b7c54/blegattcoroutines/src/main/java/com/beepiz/bluetooth/gattcoroutines/experimental), so you can already try it out in your project pretty easily. 
