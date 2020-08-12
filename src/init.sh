#!/bin/bash

# This script populates /data, when the container is started first
# Data is expected to be a file system bind mount. Hence, it cannot be populated using Docker's volume population method

# Create /data/etc if not existing
if [ ! -d /data/etc/ ]; then
  echo "Create data directory"
	mkdir -p /data/etc
fi	

if [ ! -d /data/etc/tls ]; then
	echo "Create tls directory"
	mkdir /data/etc/tls;
fi

if [ ! -d /data/etc/easy-rsa ]; then
  echo "Initialize CA"
	cp -a /usr/share/easy-rsa /data/etc
	$( cd /data/etc/easy-rsa && ./easyrsa init-pki  && ./easyrsa build-ca nopass --req-cn=$(hostname))
	ln -s -f ../easy-rsa/pki/ca.crt  /data/etc/tls/ca.crt
fi

if [ ! -f /data/etc/easy-rsa/pki/private/$(hostname).key ]; then
  echo "Create server certificate for $(hostname)"
	$( cd /data/etc/easy-rsa && ./easyrsa build-server-full $(hostname) nopass)
	ln -s -f ../easy-rsa/pki/private/$(hostname).key /data/etc/tls/local.key
  ln -s -f ../easy-rsa/pki/issued/$(hostname).crt /data/etc/tls/local.crt
fi

if [ ! -f /data/etc/mosquitto_passwd ]; then
  passwd=$(makepasswd --chars=8)
  echo "Initialize Mosquitto passwd - admin:$passwd"
  touch /data/etc/mosquitto_passwd
  mosquitto_passwd -b /data/etc/mosquitto_passwd admin $passwd
fi

if [ ! -d /data/webdav ]; then
  echo "Create WebDAV directory"
  mkdir /data/webdav
  chown www-data:www-data /data/webdav
fi

if [ ! -d /data/etc/apache2 ]; then
  echo "Copy apache2 configuration"
  mkdir /data/etc/apache2
  cp /etc/apache2/sites-available/httpd.conf /data/etc/apache2

  echo "Generate cryptographic data for Apache2"
  openssl ecparam -name secp256k1 -genkey -noout -out /data/etc/apache2/ec-priv.pem
  openssl ec -in /data/etc/apache2/ec-priv.pem -pubout -out /data/etc/apache2/ec-pub.pem
  chown www-data:www-data /data/etc/apache2/*.pem

  passwd=$(makepasswd --chars=8)
  stpasswd=$(makepasswd --chars=8)
  echo "Initialize Apache2 passwd - admin:$passwd"
  htpasswd -c -b /data/passwd admin $passwd
  echo "Initialize Apache2 passwd - stone:$stpasswd"
  htpasswd -b /data/passwd admin $stpasswd
  echo -e "admins: admin\nstones: stone" > /data/group
fi

if [ ! -d /data/log/apache2 ]; then
  echo "Create apache log directory"
  mkdir -p /data/log/apache2
  chown -R www-data:www-data /data/log/apache2
fi

if [ ! -d /data/log/mosquitto ]; then
  echo "Generate /data/log/mosquitto"
  mkdir /data/log/mosquitto
  chown mosquitto:mosquitto /data/log/mosquitto
fi

if [ ! -d /data/lib/mosquitto ]; then
  echo "Generate /data/lib/mosquitto"
  mkdir -p /data/lib/mosquitto
  chown mosquitto:mosquitto /data/lib/mosquitto
fi


service apache2 start
service mosquitto start

# Start an interactive shell in case sombody attaches
/bin/bash
