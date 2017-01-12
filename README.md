# anycall
This project can run as root to call system service IPC from normal apps.

You can find the native part [here](https://github.com/ztc1997/anycall-native)

## Use
This module `simple` demonstrates how to call the `PowerManager.goToSleep(long uptimeMillis)` to
turn the screen off.

1. Import
Available on jcenter
```
compile 'com.ztc1997.anycall:library:x.y.z'
```

2. Initializes an Anycall instance
```
Anycall anycall = new Anycall(this);
anycall.startShell(new Anycall.StartShellListener() {
    @Override
    public void onFinish(boolean success) {
        output.append("Start shell success = " + success + "\n");
    }
});
```

3. Calling the IPC method
```
anycall.callMethod("android.os.IPowerManager", POWER_SERVICE, "goToSleep",
       SystemClock.uptimeMillis(), new Anycall.CallMethodResultListener() {
           @Override
           public boolean onResult(int resultCode, Parcel reply) {
               output.append("Go to sleep resultCode = " + resultCode + "\n");
               Log.d(TAG, "resultCode = " + resultCode);
               try {
                   reply.readException();
               } catch (Exception e) {
                   e.printStackTrace();
               }
               return true;
           }
       });
```

4. Recycle resources
```
anycall.stopShell();
```

## known bugs
1. Because there is no time to compile the binaries, temporarily can only use in Android 6.0-7.1.
