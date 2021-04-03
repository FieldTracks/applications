#!/bin/sh

mkdir -p root/usr/local/bin
mkdir -p root/usr/share/stonesimulator

cp ../simulator.py root/usr/local/bin/stonesimulator
cp ../requirements.txt root/usr/share/stonesimulator/

chmod go-rwx root/etc/stonesimulator/config

sed -i s/##BUILDNR##/${BUILD_NUMBER}/ root/DEBIAN/control

mv root stonesimulator_1.0-${BUILD_NUMBER}

fakeroot dpkg-deb --build stonesimulator_1.0-${BUILD_NUMBER}
