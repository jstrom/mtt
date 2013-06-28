#!/bin/bash

adb shell setprop debug.assert 1
adb -d install -r bin/MttMain-debug.apk
adb -d shell am start -n com.mtt/com.mtt.MttMain
