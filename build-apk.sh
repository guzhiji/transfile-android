#!/usr/bin/env bash

apkfile='app/build/outputs/apk/release/app-release.apk'

./gradlew assembleRelease

if [ -f "$apkfile" ] ; then
	echo
	echo "$apkfile"
	echo
fi

