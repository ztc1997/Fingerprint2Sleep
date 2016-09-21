/*
 * Copyright (c) 2016 Juan García Basilio
 *
 * This file is part of WaveUp.
 *
 * WaveUp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WaveUp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WaveUp.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jarsilio.android.waveup;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;

public class Root {
    private static final String TAG = "Root";

    public static void pressPowerButton() {
        executeAsRoot("input keyevent 26");
    }

    public static boolean requestSuPermission() {
        return executeAsRoot("");
    }

    private static boolean executeAsRoot(String command) {
        Log.d(TAG, "Trying to execute '" + command + "' as root");
        boolean accessGranted = false;
        try {
            Process suProcess = Runtime.getRuntime().exec("su");
            DataOutputStream dataOutputStream = new DataOutputStream(suProcess.getOutputStream());
            dataOutputStream.writeBytes(command + "\n");
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            int suProcessReturnValue = suProcess.waitFor();
            if (suProcessReturnValue == 0) {
                Log.d(TAG, "Root access granted (return value " + suProcessReturnValue + ")");
                accessGranted = true;
            } else {
                Log.d(TAG, "Root access denied (return value " + suProcessReturnValue + ")");
            }
        } catch (IOException | SecurityException e) {
            Log.w(TAG, "Couldn't get root access while executing '" + command + "'", e);
        } catch (InterruptedException e) {
            Log.w(TAG, "Error trying to get root access", e);
        }
        return accessGranted;
    }
}