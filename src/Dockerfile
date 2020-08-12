# This system relies on Apache2 handing out JWT tockens
# Unfortunatly, binary builds from mod_authnz_jwt are not included in debian
# Therefore, we create a container for building the module on a Debian Buster system
# It uses buster-backports, because checkinstall is not included in buster

FROM debian:buster-backports AS build-apache2-jwt
RUN apt-get update && apt-get install -y libtool \
    pkg-config \
    autoconf \
    libssl-dev \
    check \
    libjansson-dev \
    git \
    build-essential \
    checkinstall \
    apache2 \
    apache2-dev \
    libz-dev
RUN cd / && git clone https://github.com/benmcollins/libjwt && \
    cd libjwt && \
    git checkout tags/v1.12.0 && \
    autoreconf -i && \
    ./configure --prefix=/usr && \
    make && \
    checkinstall --pkgname=fieldtracks-libjwt --default && \
    make install
RUN cd / && git clone https://github.com/AnthonyDeroche/mod_authnz_jwt && \
    cd mod_authnz_jwt && \
    autoreconf -ivf && \
    ./configure --prefix=/usr && \
    make && \
    checkinstall --pkgname=fieldtracks-mod_authnz_jwt --default

# This is the actual container for a fieldtracks env
# This container runs both mqtt and webdav service
FROM debian:buster
MAINTAINER Fieldtracks Project <info@fieldtracks.org>
RUN apt-get update && apt-get upgrade -y && apt-get -y install \
	net-tools \
	vim \
	apache2 \
	mosquitto \
	apt-utils \
	easy-rsa \
	makepasswd
COPY --from=build-apache2-jwt /libjwt/*.deb /tmp
COPY --from=build-apache2-jwt /mod_authnz_jwt/*.deb /tmp
RUN dpkg -i /tmp/*.deb && \
    a2enmod dav && \
    a2enmod dav_fs && \
    a2enmod ssl && \
    a2enmod auth_jwt && \
    a2enmod authz_groupfile  && \
    rm /etc/apache2/sites-enabled/* && \
    ln -s /data/etc/apache2/httpd.conf /etc/apache2/sites-enabled/httpd.conf && \
    rm -r /var/log/mosquitto && \
    ln -s /data/log/mosquitto /var/log/mosquitto

COPY mosquitto.conf /etc/mosquitto/conf.d
COPY init.sh /usr/local/sbin/init.sh
COPY vars /usr/share/easy-rsa
COPY openssl-easyrsa.cnf /usr/share/easy-rsa
COPY httpd.conf /etc/apache2/sites-available

EXPOSE 80/tcp
EXPOSE 1883/tcp
EXPOSE 8883/tcp

ENTRYPOINT ["/usr/local/sbin/init.sh"]
VOLUME /data
#USER fieldtracks

