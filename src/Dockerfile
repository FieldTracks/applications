# This system relies on Apache2 handing out JWT tockens
# Unfortunatly, binary builds from mod_authnz_jwt are not included in debian
# Therefore, we create a container for building the module on a Debian Buster system
# It uses buster-backports, because checkinstall is not included in buster

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
    libmosquitto-dev \
    npm
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
RUN cd / && git clone https://github.com/FieldTracks/fieldmon.git && \
    npm install -g @angular/cli && \
    cd /fieldmon && \
    npm install && ng build --configuration=docker
RUN cd / && git clone https://github.com/FieldTracks/StoneAggregator.git && \
    cd StoneAggregator/packaging && \
    BUILD_NUMBER=$(date +%s) ./build.sh
RUN cd / && git clone https://github.com/FieldTracks/StoneSimulator.git && \
    cd StoneSimulator/packaging && \
    BUILD_NUMBER=$(date +%s) ./build.sh
RUN cd / && git clone https://github.com/FieldTracks/StoneFlashtool.git && \
    cd StoneFlashtool/packaging && \
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
	libmariadb-dev-compat
COPY --from=build /mosquitto-go-auth/go-auth.so /usr/local/lib
COPY --from=build /fieldmon/dist/* /var/www/html

COPY --from=build "/StoneAggregator/packaging/*.deb" "/StoneFlashtool/packaging/*.deb" "/libjwt/*.deb" "/mod_authnz_jwt/*.deb" /tmp/
RUN dpkg -i /tmp/*.deb && \
    a2enmod dav && \
    a2enmod dav_fs && \
    a2enmod ssl && \
    a2enmod auth_jwt && \
    a2enmod authz_groupfile  && \
    a2enmod rewrite  && \
    rm /etc/apache2/sites-enabled/* && \
    ln -s /data/etc/apache2/vhosts.conf /etc/apache2/sites-enabled/vhosts.conf && \
    rm -r /var/log/mosquitto && \
    ln -s /data/log/mosquitto /var/log/mosquitto && \
    rm -r /var/lib/mosquitto && \
    ln -s /data/lib/mosquitto /var/lib/mosquitto && \
    rm -r /etc/ssh/ssh_host_* && \
    mkdir /root/.ssh/ && \
    mkdir -p /usr/local/share/ca-certificates/fieldtracks && \
    ln -s /data/etc/tls/web.ca.crt /usr/local/share/ca-certificates/fieldtracks/web.ca.crt && \
    ln -s /data/etc/tls/ca.crt /usr/local/share/ca-certificates/fieldtracks/ca.crt

COPY mosquitto.conf /etc/mosquitto/conf.d
COPY init.sh /usr/local/sbin/init.sh
COPY vars openssl-easyrsa.cnf /usr/share/easy-rsa/
COPY apache/vhosts.conf /etc/apache2/sites-available
COPY apache/fieldtracks.conf /etc/apache2/conf-available/
COPY apache/check_bearer.json /var/www/api/check_bearer.json
COPY stoneaggregator.config.ini /usr/local/etc

EXPOSE 80/tcp 1883/tcp 8883/tcp 22/tcp 443/tcp

ENTRYPOINT ["/usr/local/sbin/init.sh"]
VOLUME /data
#USER fieldtracks

