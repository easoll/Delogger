package com.easoll.delogger

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val hello = "hello world"
        val name = "easoll"

        Log.i(TAG, "log content is " +
                "name: $name, hello: $hello, kotlin file")

        Log.i(TAG, "log content is name: $name, hello: $hello, kotlin file")

        Toast.makeText(this, "$name, $hello", Toast.LENGTH_SHORT).show()

        JavaFile.sayHello(this)

        Delogger.i(TAG, "message from Delogger, kotlin file")
    }
}
