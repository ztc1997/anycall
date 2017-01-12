# anycall
This project can run as root to call system service IPC from normal apps.

## Use
This module `simple` demonstrates how to call the `PowerManager.goToSleep(long uptimeMillis)` to
turn the screen off.

1. Initializes an Anycall instance
```
Anycall anycall = new Anycall(this);
anycall.startShell(new Anycall.StartShellListener() {
    @Override
    public void onFinish(boolean success) {
        output.append("Start shell success = " + success + "\n");
    }
});
```

2. Calling the IPC method
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

3. Recycle resources
```
anycall.stopShell();
```