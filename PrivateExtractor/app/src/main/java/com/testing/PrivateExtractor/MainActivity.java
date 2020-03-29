/*
 * Copyright 2015, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.testing.PrivateExtractor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;


/**
 * An {@link Activity} that gets a text string from the user and displays it back when the user
 * clicks on one of the two buttons. The first one shows it in the same activity and the second
 * one opens another activity and displays the message.
 */
public class MainActivity extends Activity {
    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if ("string/txt".equals(type) || "application/json".equals(type) ) {
                receiveSharedData(intent); // Handle text being sent
            } else {
                toast("This is not a valid export");
            }
        } else {
            toast("Don't use Private Extractor directly.");
        }
    }

    public void receiveSharedData(Intent intent) {
        /* Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                System.out.println(key + " : " + (bundle.get(key) != null ? bundle.get(key) : "NULL"));
            }
        } */

        ArrayList<Uri> sharedText = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (sharedText != null) {
            String data = "";
            for (int i = 0; i < sharedText.size(); i++) {
                InputStream is = null;
                try {
                    is = getContentResolver().openInputStream(sharedText.get(i));
                } catch (FileNotFoundException e) {
                    System.out.println("Could not find stream.");
                    continue; // Safe since we couldn't open it.
                }

                try {
                    String output = inputStreamToString(is, "UTF-8");
                    data += output;
                } catch (IOException e) {
                    System.out.println("Could not turn stream -> string");
                }


                try {
                    is.close();
                } catch (IOException e) {
                    System.out.println("Could not close stream.");
                }
            }
            writeToFile(data, getApplicationContext());
        } else {
            toast("Private Extractor received no data.");
        }
    }

    private void writeToFile(String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("hot.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
            toast("Private Extract extraction completed.");
        }
        catch (IOException e) {
            toast("Private Extractor extraction failed.");
        }
    }

    private String inputStreamToString(InputStream stream, String charsetName) throws IOException
    {
        int n = 0;
        char[] buffer = new char[1024 * 4];
        InputStreamReader reader = new InputStreamReader(stream, charsetName);
        StringWriter writer = new StringWriter();
        while (-1 != (n = reader.read(buffer))) writer.write(buffer, 0, n);
        return writer.toString();
    }

    private void toast(String input) {
        Context context = getApplicationContext();
        CharSequence text = input;
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}
