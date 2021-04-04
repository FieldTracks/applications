#!/bin/bash

# This script populates /data, when the container is started first
# Data is expected to be a file system bind mount. Hence, it cannot be populated using Docker's volume population method

# Create /data/etc if not existing
if [ ! -d /data/etc/ ]; then
  echo "Create data directory"
	mkdir -p /data/etc
fi

# etc/tls contains symlinks the the certificate used by this server
# These certificates are used by mqtt (mosquitto) and https (apache)
if [ ! -d /data/etc/tls ]; then
	echo "Create tls directory"
	mkdir /data/etc/tls;
fi

# Directory for mqtt configuration
if [ ! -d /data/etc/mqtt ]; then
	echo "Create mqtt directory"
	mkdir /data/etc/mqtt;
fi


# Internal certificates are generated using RSA, i.e. a small, command line CA for internal usage
# It is placed in etc/easy-rsa
if [ ! -d /data/etc/easy-rsa ]; then
  echo "Initialize CA"
	cp -a /usr/share/easy-rsa /data/etc
	$( cd /data/etc/easy-rsa && ./easyrsa init-pki  && ./easyrsa build-ca nopass --req-cn=$(hostname))
	ln -s -f ../easy-rsa/pki/ca.crt  /data/etc/tls/ca.crt
fi

# Certificates for websocket use
if [ ! -f /data/etc/easy-rsa/pki/private/$(hostname).key ]; then
  echo "Create server certificate for $(hostname)"
	$( cd /data/etc/easy-rsa && ./easyrsa build-server-full $(hostname) nopass)
	ln -s -f ../easy-rsa/pki/private/$(hostname).key /data/etc/tls/local.key
  ln -s -f ../easy-rsa/pki/issued/$(hostname).crt /data/etc/tls/local.crt
  chmod 755 /data/etc/easy-rsa/pki/
fi

# Stones authenticate not using mqtt. They provide a simple password
# In general, we don't expect people messing with stones; this is outside the threat-model
# However, as we're distributing plain text password for use in esp-idf, it could be helpful
# To restrict stones using a dedicated mqtt ACL
if [ ! -f /data/etc/mqtt/mosquitto_passwd ]; then
  passwd=$(makepasswd --chars=8)
  echo "Initialize Mosquitto - stone:$passwd"
  touch /data/etc/mqtt/mosquitto_passwd
  mosquitto_passwd -b /data/etc/mqtt/mosquitto_passwd stone $passwd
  echo $passwd > /data/etc/mqtt/mqtt_passwd_stone_clear.txt
fi

# WebDAV file share. Used for all kinds of files
if [ ! -d /data/webdav ]; then
  echo "Create WebDAV directory"
  mkdir /data/webdav
  chown www-data:www-data /data/webdav
fi

# Apache configuration. Apache provides a WebDAV share and serves the application
if [ ! -d /data/etc/apache2 ]; then
  echo "Copy apache2 configuration"
  mkdir /data/etc/apache2
  cp /etc/apache2/sites-available/vhosts.conf /data/etc/apache2

  echo "Generate cryptographic data for Apache2 / JWT"
  openssl ecparam -name secp256k1 -genkey -noout -out /data/etc/apache2/ec-priv.pem
  openssl ec -in /data/etc/apache2/ec-priv.pem -pubout -out /data/etc/apache2/ec-pub.pem
  chown www-data:www-data /data/etc/apache2/*.pem

  passwd=$(makepasswd --chars=8)
  echo "Initialize Apache2 passwd - admin:$passwd"
  htpasswd -c -b /data/passwd admin $passwd
fi

# Both apache and moquitto are supposed to log at this share
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

# Persist stones in this topic
if [ ! -d /data/lib/mosquitto ]; then
  echo "Generate /data/lib/mosquitto"
  mkdir -p /data/lib/mosquitto
  chown mosquitto:mosquitto /data/lib/mosquitto
fi

dpkg-reconfigure -f openssh-server #Generate server keys
update-ca-certificates # User could have changed key material

service apache2 start
service mosquitto start
service ssh start
/usr/local/bin/stoneaggregator /usr/local/etc/stoneaggregator.config.ini
