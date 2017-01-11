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

    public static final int ERROR_CANNOT_OBTAIN_TRANSACTION_CODE = -1;
    public static final int ERROR_NO_SERVICE_NAME = 1;
    public static final int ERROR_FAILED_TO_GET_SERVICE_MANAGER = 2;
    public static final int ERROR_FAILED_TO_GET_SERVICE = 3;

    private LruCache<String, Integer> cache = new LruCache<String, Integer>(1024 * Integer.SIZE) {
        @Override
        protected int sizeOf(String key, Integer value) {
            return Integer.SIZE;
        }
    };

    private File binaryFile;

    private Shell.Interactive rootSession;

    private int commandCount = 1;

    public Anycall(@NonNull final Context ctx) {
        binaryFile = new File(ctx.getFilesDir(), "anycall");
        copyFileIfNotExist(ctx.getAssets());
    }

    public synchronized boolean startShell(@Nullable final StartShellListener listener) {
        if (rootSession != null && rootSession.isRunning()) {
            return true;
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

        return false;
    }

    public synchronized void stopShell() {
        rootSession.close();
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
                    Parcel reply = Parcel.obtain();
                    if (exitCode == 0) {
                        String replyBase64 = output.get(0);
                        byte[] replyRaw = Base64.decode(replyBase64, Base64.NO_WRAP);
                        reply.unmarshall(replyRaw, 0, replyRaw.length);
                    }
                    listener.onResult(exitCode, reply);
                    reply.recycle();
                }
            }
        });
    }

    public void callMethod(final String className, final String serviceName,
                           final String methodName, final Object... paramsAndListener) {
        Parcel data = Parcel.obtain();
        data.writeInterfaceToken("android.os.IPowerManager");
        for (int i = 0; i < paramsAndListener.length - 1; i++) {
            Object p = paramsAndListener[i];
            if (p instanceof Byte)
                data.writeByte((Byte) p);
            else if (p instanceof Integer)
                data.writeLong((Integer) p);
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
        CallMethodResultListener listener = lastParam == null ? null : (CallMethodResultListener) lastParam;

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
            final Class<?> cls = Class.forName("android.os.IPowerManager$Stub");
            final Field declaredField = cls.getDeclaredField("TRANSACTION_goToSleep");
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
        return binaryFile.exists() ||
                AssetUtil.copyAssets(am, BuildCompat.chooseAbi() + "/anycall",
                        binaryFile.getAbsolutePath());
    }

    public interface StartShellListener {
        void onFinish(boolean success);
    }

    public interface CallMethodResultListener {
        void onResult(int resultCode, Parcel reply);
    }
}
