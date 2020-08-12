Ths git repo contains all files needed to create a Docker image hosting an environment.
Typically, environments can be started in the cloud or locally.

`run.sh` provides an example for starting a Docker container. It executes:

```bash
docker run \
  --name fieldtracks \
  --hostname local-dev.fieldtracks.org \
  --rm \
  -it \
  -p 8485:80 \
  --mount type=bind,source="$(pwd)"/data,target=/data \
  fieldtracks:latest
```

The data-directory contains the configuration, the key material and messages persisted message in mosquitto. 
The latter is not implemented, yet.

An [easy-rsa](https://github.com/OpenVPN/easy-rsa) base certificate authority (CA) is generated during boot. 
Its key material is used by JellingStone devices. Hence, it is also used by mosquittos mqtt listeners.

The image also starts an apache2-deamon handing out JWT tokens. Apache2 does not use TLS, as this would require universal 
valid certificates (e.g. based on letsencrypt). However, this docker image is designed not to rely
on external connectivity (e.g. for setting up local development environments). In consequence, it is assumed
that TLS is implemented by a 3rd party container; for both MQTT/websocket and HTTP. (TBD: Propose a 3rd party container for doing so.)

The data-volume is assumed to be bound to a directory, because some files can be edited by hand:
* `data/passwd` and `data/group` list all user accounts, which are valid in the system. Typically,
creating users results in modifying `passwd`, whereas promoting users to be admins is done in `group`. This files 
are edited by htpasswd.
In general, admins, users and stones have different permission on the various mqtt-topis. 
* `data/etc/tls` contains links to the tls certificates used by mosquitto. Changing these results in a different mosquitto configuration
* `data/etc/mosquitto_passwd` contains accounts for mosquitto. When mosquitto is going to utilizes jwt-tokens, this file is not needed anymore.

## Building the container 
The container is build by executing `make`. Corresponding sources are available in `./src`.  