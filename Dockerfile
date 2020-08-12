FROM debian:buster
MAINTAINER Fieldtracks Project <info@fieldtracks.org>
RUN useradd -m -s /bin/bash fieldtracks 

VOLUME /data
#USER fieldtracks
