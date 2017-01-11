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

package com.ztc1997.anycall.util;

import android.os.Build;

import java.util.Arrays;
import java.util.List;

public class BuildCompat {
    private BuildCompat() {
    }

    public static String chooseAbi() {
        if (Build.VERSION.SDK_INT >= 21) {
            List<String> abis = Arrays.asList(Build.SUPPORTED_ABIS);

            if (abis.contains("x86_64")) {
                return "x86_64";
            } else if (abis.contains("x86")) {
                return "x86";
            } else if (abis.contains("arm64-v8a")) {
                return "arm64";
            } else if (abis.contains("armeabi") || abis.contains("armeabi-v7a")) {
                return "arm";
            } else {
                throw new IllegalStateException("Unsupported ABI");
            }
        }

        //noinspection deprecation
        switch (Build.CPU_ABI) {
            case "x86_64":
                return "x86_64";
            case "x86":
                return "x86";
            case "arm64-v8a":
                return "arm64";
            case "armeabi":
            case "armeabi-v7a":
                return "arm";
            default:
                throw new IllegalStateException("Unsupported ABI");
        }
    }
}
