package com.easoll.delogger;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class JavaFile {
    private static final String TAG = MainActivity.TAG;

    public static void sayHello(Context context){
        Log.i(TAG, "log content is " +
                "name: $name, hello: $hello");

        Log.i(TAG, "log content is " + "name: $name, hello: $hello");

        Toast.makeText(context, "$name, $hello", Toast.LENGTH_SHORT).show();

        Delogger.i(TAG, "message from Delogger");
    }
}
