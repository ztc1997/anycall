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

import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetUtil {
    private AssetUtil() {
    }

    public static boolean compareAndCopyAssets(AssetManager am, String in, String out) {
        Log.d("AssetUtil", "compareAndCopyAssets");
        InputStream ais = null;
        byte[] src;
        try {
            ais = new BufferedInputStream(am.open(in));
            src = new byte[ais.available()];
            if (ais.read(src) < 0) return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (ais != null) try {
                ais.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File outFile = new File(out);
        if (compare(src, outFile)) return true;

        outFile.delete();
        OutputStream oos = null;
        try {
            oos = new BufferedOutputStream(new FileOutputStream(outFile));
            oos.write(src);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (oos != null) try {
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    /*public static boolean copyAssets(AssetManager am, String in, String out) {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = am.open(in);

            os = new FileOutputStream(out);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null)
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return false;
    }*/

    private static boolean compare(byte[] src, File file) {
        Log.d("AssetUtil", "compare");
        if (!file.exists()) return false;
        InputStream is = null;
        try {
            int size = src.length;
            is = new FileInputStream(file);
            if (is.available() != size) return false;

            for (byte sb : src) {
                byte b = (byte) is.read();
                if (sb != b) return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
