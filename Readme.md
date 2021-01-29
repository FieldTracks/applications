This git repo contains all files needed to create a Docker image hosting an environment.
Typically, environments can be started in the cloud or locally.

`run.sh` provides an example for starting a Docker container. It executes:

```bash
docker run \
  --name fieldtracks \
  --hostname local-dev.fieldtracks.org \
  --rm \
  -it \
  -p 8485:80 \
  -p 8443:443 \
  -p 2225:22 \
  -p 2883:1883 \
  -p 8883:8883 \
  --mount type=bind,source="$(pwd)"/data,target=/data \
  fieldtracks:latest
```

The data-directory contains the configuration, the key material and messages persisted message in mosquitto.
During start, a the resulting Docker container generates and logs random password for mqtt users (stone, admin). 

*Note: A valid TLS certificate (Let's encrypt) for local-dev.fieldtracks.org is privatly available at https://git.freifunk-koeln.de/FieldTracks/local-dev-cert/-/jobs/artifacts/master/browse?job=deploy*.

An [easy-rsa](https://github.com/OpenVPN/easy-rsa) base certificate authority (CA) is generated during boot. 
Its key material is used by JellingStone devices. Hence, it is also used by mosquittos mqtt listeners and apache2.

The resulting image starts an apache2-deamon handing out JWT tokens. By default, Apache's TLS listener also uses
the certificates provided by easy-rsa. To use a valid certificate, you can either install the easy-rsa one in your local
browser (preferred for a development enviroment) or utilize valid TLS certificates gathered from Let's encrypt.
The latter requires an additional container (for production use, 
e.g. https://github.com/matrix-org/docker-dehydrated).


The data-volume is designed to be bound to a directory, because some files can be edited or read by hand:
* `passwd` list all user accounts, which are valid for jwt. It is edited through htpasswd.
In general, admins, users and stones have different permission on the various mqtt-topis. 
* `etc/tls` contains links to the TLS certificates used by mosquitto. Changing these results in a different mosquitto configuration
* `etc/mqtt/mosquitto_passwd` contains accounts for mosquitto. When mosquitto is going to utilizes jwt-tokens, this file is not needed anymore.
* `etc/mqtt/mqtt_passwd_stone_clear.txt` has
* `etc/apache/vhosts.conf` has Apache's vhost configuration. 
* `log/` contains log-output from both mosquitto and apache2.

## SSH-Configuration

The resulting container generates SSH-keys on boot. Persisting keys across container re-recreation
requires addtional volumes, but avoid bind-mounts to maintain the existing configuration.
```
--mount source=ft-ssh,destination=/etc/ssh \
```
However, a bind mount can be ideal to hook in your personal SSH-key; it must belong to root.
``` 
--mount type=bind,source=/path/to/.ssh/id_rsa.pub,destination=/root/.ssh/authorized_keys \
```


## Building the container 
The container is build by executing `make`. Corresponding sources are available in `./src`.  
