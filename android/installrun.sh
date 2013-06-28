#!/bin/bash

adb shell setprop debug.assert 1
adb -d install -r bin/VxaViewer-debug.apk
adb -d shell am start -n april.android/april.android.VxaViewer
