# This system relies on Apache2 handing out JWT tockens
# Unfortunatly, binary builds from mod_authnz_jwt are not included in debian
# Therefore, we create a container for building the module on a Debian Buster system
# It uses buster-backports, because checkinstall is not included in buster

FROM maven:3.8-openjdk-11 as javabuild
COPY middleware.foo /middleware
RUN cd /middleware && mvn install


FROM debian:buster-backports AS build
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
    libz-dev \
    gccgo \
    golang \
    mosquitto-dev \
    libmosquitto-dev
# File missing in mosquitto-dev as of Debian Buster
# This hack is no longer needed for 1.6.9 (currently in Debian Sid)
ADD https://raw.githubusercontent.com/eclipse/mosquitto/1.5.x/src/mosquitto_broker.h /usr/include

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
RUN cd / && git clone https://github.com/iegomez/mosquitto-go-auth.git && \
    cd /mosquitto-go-auth && \
    CGO_CFLAGS="-I/usr/include -fPIC" CGO_LDFLAGS="-shared" make


FROM node:14 as build-angular
RUN npm install -g @angular/cli@v10
COPY fieldmon /fieldmon
RUN cd /fieldmon && \
    npm install && \
    ng build --configuration=docker

FROM debian:buster as build-fieldmon
RUN apt-get update && apt-get install -y npm \
    git

FROM python:3.9-buster as python-build
RUN apt update && apt install -y fakeroot

COPY StoneAggregator /StoneAggregator
RUN cd /StoneAggregator/packaging && \
    BUILD_NUMBER=$(date +%s) ./build.sh

COPY StoneSimulator /StoneSimulator
RUN cd /StoneSimulator/packaging && \
    BUILD_NUMBER=$(date +%s) ./build.sh

COPY StoneFlashtool /StoneFlashtool
RUN cd /StoneFlashtool/packaging && \
    BUILD_NUMBER=$(date +%s) ./build.sh


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
	makepasswd \
	openssh-server \
	python3-pip \
	libmariadbclient-dev \
	libmariadb-dev-compat \
  openjdk-11-jre
COPY --from=build /mosquitto-go-auth/go-auth.so /usr/local/lib
COPY --from=build "/libjwt/*.deb" "/mod_authnz_jwt/*.deb" /tmp/

COPY --from=javabuild "/middleware/target/middleware-*.jar" /middleware.jar

COPY --from=build-angular /fieldmon/dist/* /var/www/html
COPY --from=python-build "/StoneSimulator/packaging/*.deb" "/StoneAggregator/packaging/*.deb" "/StoneFlashtool/packaging/*.deb" /tmp/
RUN dpkg -i /tmp/*.deb && \
    a2enmod dav && \
    a2enmod dav_fs && \
    a2enmod ssl && \
    a2enmod auth_jwt && \
    a2enmod authz_groupfile  && \
    a2enmod rewrite  && \
    a2enmod proxy && \
    a2enmod proxy_http  && \
    a2enmod proxy_wstunnel  && \
    a2enmod deflate  && \
    rm /etc/apache2/sites-enabled/* && \
    ln -s /data/etc/apache2/vhosts.conf /etc/apache2/sites-enabled/vhosts.conf && \
    rm -r /var/log/mosquitto && \
    ln -s /data/log/mosquitto /var/log/mosquitto && \
    rm -r /var/lib/mosquitto && \
    ln -s /data/lib/mosquitto /var/lib/mosquitto && \
    rm -r /etc/ssh/*key* && \
    mkdir /root/.ssh/ && \
    mkdir -p /usr/local/share/ca-certificates/fieldtracks && \
    ln -s /data/etc/tls/web.ca.crt /usr/local/share/ca-certificates/fieldtracks/web.ca.crt && \
    ln -s /data/etc/tls/ca.crt /usr/local/share/ca-certificates/fieldtracks/ca.crt

COPY configs/mosquitto.conf /etc/mosquitto/conf.d
COPY init.sh /usr/local/sbin/init.sh
COPY configs/vars configs/openssl-easyrsa.cnf /usr/share/easy-rsa/
COPY configs/apache/vhosts.conf /etc/apache2/sites-available
COPY configs/apache/fieldtracks.conf /etc/apache2/conf-available/
COPY configs/apache/check_bearer.json /var/www/api/check_bearer.json
COPY configs/stoneaggregator.config.ini /usr/local/etc

EXPOSE 80/tcp 1883/tcp 8883/tcp 22/tcp 443/tcp

ENTRYPOINT ["/usr/local/sbin/init.sh"]
VOLUME /data
#USER fieldtracks
