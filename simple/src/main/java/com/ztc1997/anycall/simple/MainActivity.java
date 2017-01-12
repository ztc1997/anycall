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

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.ztc1997.anycall.Anycall;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    private Anycall anycall;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final AppCompatButton btn = (AppCompatButton) findViewById(R.id.btn_go_to_sleep);
        final TextView output = (TextView) findViewById(R.id.output);

        anycall = new Anycall(this);
        anycall.startShell(new Anycall.StartShellListener() {
            @Override
            public void onFinish(boolean success) {
                output.append("Start shell success = " + success + "\n");
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        anycall.stopShell();
    }
}
