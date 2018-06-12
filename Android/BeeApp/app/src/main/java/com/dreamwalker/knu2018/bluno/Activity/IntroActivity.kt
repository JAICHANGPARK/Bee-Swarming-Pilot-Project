package com.dreamwalker.knu2018.bluno.Activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity

import com.dreamwalker.knu2018.bluno.MainActivity
import com.dreamwalker.knu2018.bluno.R

import java.util.ArrayList

class IntroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        val permissionCoarse = if (Build.VERSION.SDK_INT >= 23)
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        else
            PackageManager.PERMISSION_GRANTED

        if (permissionCoarse == PackageManager.PERMISSION_GRANTED) {

            startActivity(Intent(this@IntroActivity, MainActivity::class.java))
            finish()
            //scanLeDevice(true);
        } else {
            askForCoarseLocationPermission()
        }
    }


    //        requirePermission();
    //
    //        boolean camera = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    //        boolean write = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    //
    //        if(camera && write){
    //            startActivity(new Intent(IntroActivity.this, MainActivity.class));
    //            finish();
    //            //사진찍은 인텐트 코드 넣기
    ////            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    ////            startActivityForResult(intent,0);
    //        }else {
    //            Toast.makeText(IntroActivity.this, "위치 권한 및 쓰기 권한을 주지 않았습니다.", Toast.LENGTH_SHORT).show();
    //        }


    private fun askForCoarseLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSIONS -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //scanLeDevice(true);
                    startActivity(Intent(this@IntroActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun requirePermission() {

        val permissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val listPermissionsNeeded = ArrayList<String>()

        for (permission in permissions) {

            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                //권한이 허가가 안됬을 경우 요청할 권한을 모집하는 부분
                listPermissionsNeeded.add(permission)
            }

        }

        if (!listPermissionsNeeded.isEmpty()) {
            //권한 요청 하는 부분
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray<String>(), 1)
        }


    }

    companion object {

        private val REQUEST_LOCATION_PERMISSIONS = 2
    }
}
