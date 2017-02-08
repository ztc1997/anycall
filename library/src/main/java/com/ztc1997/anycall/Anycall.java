/*
 * Copyright 2016-2017 Alex Zhang aka. ztc1997
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ztc1997.anycall;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.util.LruCache;

import com.ztc1997.anycall.util.AssetUtil;
import com.ztc1997.anycall.util.BuildCompat;

import java.io.File;
import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import eu.chainfire.libsuperuser.Shell;

@SuppressWarnings("TryWithIdenticalCatches")
public class Anycall {
    public static final String TAG = Anycall.class.getSimpleName();

    private static final int FIRST_ERROR_CODE = 64;

    /**
     * Failed to obtain the transaction code via reflect.
     */
    public static final int ERROR_CANNOT_OBTAIN_TRANSACTION_CODE = -1;

    /**
     * The command is missing parameters.
     */
    public static final int ERROR_MISSING_PARAMETERS = FIRST_ERROR_CODE;

    /**
     * Failed to get service manager.
     */
    public static final int ERROR_FAILED_TO_GET_SERVICE_MANAGER = FIRST_ERROR_CODE + 1;

    /**
     * Failed to get service, the service may not be running.
     */
    public static final int ERROR_FAILED_TO_GET_SERVICE = FIRST_ERROR_CODE + 2;

    private LruCache<String, Integer> cache;

    private File binaryFile;

    private Shell.Interactive rootSession;

    private int commandCount = 1;

    public Anycall(@NonNull final Context ctx) {
        this(ctx, 1024);
    }

    public Anycall(@NonNull final Context ctx, final int cacheSize) {
        binaryFile = new File(ctx.getFilesDir(), "anycall");
        copyFileIfNotExist(ctx.getAssets());

        cache = new LruCache<String, Integer>(cacheSize) {
            @Override
            protected int sizeOf(String key, Integer value) {
                return Integer.SIZE;
            }
        };
    }

    public boolean isRunning() {
        return rootSession != null && rootSession.isRunning();
    }

    public void startShell(@Nullable final StartShellListener listener) {
        if (isRunning()) {
            if (listener != null) listener.onFinish(true);
        }

        rootSession = new Shell.Builder()
                .useSU()
                .open(new Shell.OnCommandResultListener() {
                    @Override
                    public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                        if (exitCode != 0) {
                            if (listener != null)
                                listener.onFinish(false);
                        } else {
                            rootSession.addCommand("chmod 755 " + binaryFile.getAbsolutePath(), 0, new Shell.OnCommandResultListener() {
                                @Override
                                public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                                    if (listener != null)
                                        listener.onFinish(exitCode == 0);
                                }
                            });
                        }
                    }
                });
    }

    public void stopShell() {
        if (rootSession != null) {
            rootSession.close();
            rootSession = null;
        }
    }

    public void callMethod(final String className, final String serviceName,
                           final String methodName, final Parcel data,
                           final CallMethodResultListener listener) {

        Integer transactionCode = obtainTransactionCode(className, methodName);
        if (transactionCode == null) {
            if (listener != null) listener.onResult(ERROR_CANNOT_OBTAIN_TRANSACTION_CODE, null);
            return;
        }

        String dataBase64 = Base64.encodeToString(data.marshall(), Base64.NO_WRAP);

        final int commandFlag = this.commandCount++;
        callBinary(serviceName, transactionCode, dataBase64, commandFlag, new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if (commandCode != commandFlag) return;

                if (BuildConfig.DEBUG)
                    Log.d(TAG, "commandCode = " + commandCode + ", exitCode = " + exitCode + ", output = " + output);

                if (listener != null) {
                    if (exitCode == 0) {
                        StringBuilder encoded = new StringBuilder();
                        for (String s : output) {
                            encoded.append(s);
                        }

                        String replyBase64 = encoded.toString();
                        Log.d(TAG, replyBase64);
                        byte[] replyRaw = Base64.decode(replyBase64, Base64.NO_WRAP);

                        Parcel reply = Parcel.obtain();
                        reply.unmarshall(replyRaw, 0, replyRaw.length);
                        reply.setDataPosition(0);
                        boolean shouldRecycle = listener.onResult(exitCode, reply);
                        if (shouldRecycle) reply.recycle();
                    } else {
                        listener.onResult(exitCode, null);
                    }
                }
            }
        });
    }

    public void callMethod(final String className, final String serviceName,
                           final String methodName, final Object... paramsAndListener) {
        Parcel data = Parcel.obtain();
        data.writeInterfaceToken(className);
        for (int i = 0; i < paramsAndListener.length - 1; i++) {
            Object p = paramsAndListener[i];
            if (p instanceof Byte)
                data.writeByte((Byte) p);
            else if (p instanceof Integer)
                data.writeInt((Integer) p);
            else if (p instanceof Long)
                data.writeLong((Long) p);
            else if (p instanceof String)
                data.writeString((String) p);
            else if (p instanceof Bundle)
                data.writeBundle((Bundle) p);
            else if (p instanceof Float)
                data.writeFloat((Float) p);
            else if (p instanceof FileDescriptor)
                data.writeFileDescriptor((FileDescriptor) p);
            else if (p instanceof List)
                data.writeList((List) p);
            else if (p instanceof Exception)
                data.writeException((Exception) p);
            else if (p instanceof IBinder)
                data.writeStrongBinder((IBinder) p);
            else if (p instanceof Double)
                data.writeDouble((Double) p);
            else if (p instanceof Map)
                data.writeMap((Map) p);
            else if (p instanceof boolean[])
                data.writeBooleanArray((boolean[]) p);
            else if (p instanceof byte[])
                data.writeByteArray((byte[]) p);
            else if (p instanceof char[])
                data.writeCharArray((char[]) p);
            else if (p instanceof int[])
                data.writeIntArray((int[]) p);
            else if (p instanceof IBinder[])
                data.writeBinderArray((IBinder[]) p);
            else if (p instanceof double[])
                data.writeDoubleArray((double[]) p);
            else if (p instanceof Object[])
                data.writeArray((Object[]) p);
            else data.writeValue(p);
        }

        Object lastParam = paramsAndListener[paramsAndListener.length - 1];
        CallMethodResultListener listener = lastParam == null ? null :
                (CallMethodResultListener) lastParam;

        callMethod(className, serviceName, methodName, data, listener);
        data.recycle();
    }

    private Integer obtainTransactionCode(final String className, final String methodName) {
        final String stubName = className + "$Stub";
        final String fieldName = "TRANSACTION_" + methodName;
        final String key = stubName + "." + fieldName;

        Integer value = cache.get(key);
        if (value != null) return value;

        try {
            final Class<?> cls = Class.forName(stubName);
            final Field declaredField = cls.getDeclaredField(fieldName);
            declaredField.setAccessible(true);
            value = declaredField.getInt(cls);

            cache.put(key, value);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return value;
    }

    private void callBinary(final String serviceName, final int code, final String dataBase64, int flag,
                            Shell.OnCommandResultListener listener) {
        callBinary(serviceName + " " + code + " " + dataBase64, flag, listener);
    }

    private void callBinary(final String params, final int flag,
                            final Shell.OnCommandResultListener listener) {
        String command = binaryFile.getAbsolutePath() + " " + params;
        if (BuildConfig.DEBUG)
            Log.d(TAG, "command = " + command);
        rootSession.addCommand(command, flag, listener);
    }

    private boolean copyFileIfNotExist(final AssetManager am) {
        String api;
        if (Build.VERSION.SDK_INT >= 23)
            api = "sdk23-25";
        else if (Build.VERSION.SDK_INT >= 19)
            api = "sdk19-22";
        else
            throw new IllegalStateException("Unsupported SDK version " + Build.VERSION.SDK_INT);
        String assetsPath = "anycall/" + api + "/" + BuildCompat.chooseAbi() + "/anycall";

        return AssetUtil.compareAndCopyAssets(am, assetsPath, binaryFile.getAbsolutePath());
    }

    public interface StartShellListener {
        void onFinish(boolean success);
    }

    public interface CallMethodResultListener {

        /**
         * @param resultCode Equals 0 when success, or indicates an error
         * @param reply      The parcel reply from the service, may contains exceptions and return values
         * @return Auto recycle reply parcel if true.
         * @see #ERROR_CANNOT_OBTAIN_TRANSACTION_CODE
         * @see #ERROR_MISSING_PARAMETERS
         * @see #ERROR_FAILED_TO_GET_SERVICE_MANAGER
         * @see #ERROR_FAILED_TO_GET_SERVICE
         */
        boolean onResult(int resultCode, @Nullable Parcel reply);
    }
}
