/*
 * Created by yuejz on 2021/08/19.
 * Copyright 2015－2021 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.analytics.android.sdk.util;

import com.sensorsdata.analytics.android.sdk.SALog;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {
    public static void writeToFile(File outFile, String content) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(outFile);
            outputStream.write(content.getBytes());
        } catch (Exception e) {
            SALog.printStackTrace(e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    SALog.printStackTrace(e);
                }
            }
        }
    }

    public static String readFileToString(File inFile) {
        try {
            ByteArrayOutputStream os = null;
            InputStream is = new BufferedInputStream(new FileInputStream(inFile));
            try {
                os = new ByteArrayOutputStream();
                byte[] b = new byte[1024];
                int len;
                while ((len = is.read(b, 0, 1024)) != -1) {
                    os.write(b, 0, len);
                }
                return os.toString();
            } catch (IOException e) {
                SALog.printStackTrace(e);
                return null;
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException e) {
                    SALog.printStackTrace(e);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return null;
        }
    }
}
