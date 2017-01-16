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

package com.ztc1997.anycall.simple;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public interface AppOpsManagerBeans {

    /**
     * Class holding all of the operation information associated with an app.
     */
    class PackageOps implements Parcelable {
        private final String mPackageName;
        private final int mUid;
        private final List<OpEntry> mEntries;

        public PackageOps(String packageName, int uid, List<OpEntry> entries) {
            mPackageName = packageName;
            mUid = uid;
            mEntries = entries;
        }

        public String getPackageName() {
            return mPackageName;
        }

        public int getUid() {
            return mUid;
        }

        public List<OpEntry> getOps() {
            return mEntries;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mPackageName);
            dest.writeInt(mUid);
            dest.writeInt(mEntries.size());
            for (int i = 0; i < mEntries.size(); i++) {
                mEntries.get(i).writeToParcel(dest, flags);
            }
        }

        PackageOps(Parcel source) {
            mPackageName = source.readString();
            mUid = source.readInt();
            mEntries = new ArrayList<>();
            final int N = source.readInt();
            for (int i = 0; i < N; i++) {
                mEntries.add(OpEntry.CREATOR.createFromParcel(source));
            }
        }

        public static final Creator<PackageOps> CREATOR = new Creator<PackageOps>() {
            @Override
            public PackageOps createFromParcel(Parcel source) {
                return new PackageOps(source);
            }

            @Override
            public PackageOps[] newArray(int size) {
                return new PackageOps[size];
            }
        };
    }

    /**
     * Class holding the information about one unique operation of an application.
     */
    class OpEntry implements Parcelable {
        private final int mOp;
        private final int mMode;
        private final long mTime;
        private final long mRejectTime;
        private final int mDuration;
        private final int mProxyUid;
        private final String mProxyPackageName;

        public OpEntry(int op, int mode, long time, long rejectTime, int duration,
                       int proxyUid, String proxyPackage) {
            mOp = op;
            mMode = mode;
            mTime = time;
            mRejectTime = rejectTime;
            mDuration = duration;
            mProxyUid = proxyUid;
            mProxyPackageName = proxyPackage;
        }

        public int getOp() {
            return mOp;
        }

        public int getMode() {
            return mMode;
        }

        public long getTime() {
            return mTime;
        }

        public long getRejectTime() {
            return mRejectTime;
        }

        public boolean isRunning() {
            return mDuration == -1;
        }

        public int getDuration() {
            return mDuration == -1 ? (int) (System.currentTimeMillis() - mTime) : mDuration;
        }

        public int getProxyUid() {
            return mProxyUid;
        }

        public String getProxyPackageName() {
            return mProxyPackageName;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mOp);
            dest.writeInt(mMode);
            dest.writeLong(mTime);
            dest.writeLong(mRejectTime);
            dest.writeInt(mDuration);
            dest.writeInt(mProxyUid);
            dest.writeString(mProxyPackageName);
        }

        OpEntry(Parcel source) {
            mOp = source.readInt();
            mMode = source.readInt();
            mTime = source.readLong();
            mRejectTime = source.readLong();
            mDuration = source.readInt();
            mProxyUid = source.readInt();
            mProxyPackageName = source.readString();
        }

        @Override
        public String toString() {
            return "OpEntry{" +
                    "mOp=" + mOp +
                    ", mMode=" + mMode +
                    ", mTime=" + mTime +
                    ", mRejectTime=" + mRejectTime +
                    ", mDuration=" + mDuration +
                    ", mProxyUid=" + mProxyUid +
                    ", mProxyPackageName='" + mProxyPackageName + '\'' +
                    '}';
        }

        public static final Creator<OpEntry> CREATOR = new Creator<OpEntry>() {
            @Override
            public OpEntry createFromParcel(Parcel source) {
                return new OpEntry(source);
            }

            @Override
            public OpEntry[] newArray(int size) {
                return new OpEntry[size];
            }
        };
    }
}
