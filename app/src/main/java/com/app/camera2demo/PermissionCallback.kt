package com.app.camera2demo

interface PermissionCallback {
    fun permissionGranted(vararg permissions:String,requestCode:Int)
    fun permissionDenied(vararg permissions:String,requestCode:Int)
}