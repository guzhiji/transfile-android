#!/usr/bin/env bash

cd `dirname "$0"`

if [ ! -d .gradle ] ; then
	mkdir .gradle
fi

keyfile='.gradle/transfile.keystore'
configfile='.gradle/transfile.config'

if [ -f "$keyfile" ] ; then
	echo $keyfile already exists. recreate it?
	read recreate
	if [ "$recreate" -eq 'y' ] ; then
		rm -f "$keyfile"
	fi
fi

keytool -genkey -v -keystore "$keyfile" -alias transfile -keyalg RSA -keysize 2048 -validity 10000

if [ ! -f "$configfile" ] ; then
	echo '
RELEASE_STORE_FILE=../.gradle/transfile.keystore
RELEASE_STORE_PASSWORD=dev123
RELEASE_KEY_ALIAS=transfile
RELEASE_KEY_PASSWORD=dev123
' > "$configfile"

fi

