import ssl
import zlib
import json
import logging
import paho.mqtt.client as mqtt

class MQTTService:
    def __init__(self, config):
        self.on_message_hooks = []

        host = config.get('MQTT Auth', 'Hostname', fallback='127.0.0.1')
        port = config.getint('MQTT Auth', 'Port', fallback=1883)
        usetls = config.getboolean('MQTT Auth', 'UseTLS', fallback=False)
        cacert = config.get('MQTT Auth', 'CACert', fallback='server.pem')
        insecure = config.getboolean('MQTT Auth', 'Insecure', fallback=False)
        user = config.get('MQTT Auth', 'Username', fallback='Aggregator')
        passwd = config.get('MQTT Auth', 'Password', fallback='')

        self.client = mqtt.Client('MQTTService')
        self.client.username_pw_set(user, passwd)
        if usetls:
            self.client.tls_set(cacert, tls_version=ssl.PROTOCOL_TLSv1_2)
            if insecure:
                self.client.tls_insecure_set(True)
        self.client.reconnect_delay_set(min_delay=1, max_delay=30)

        self.client.on_message = self._on_message
        self.client.on_connect = self._on_connect
        self.client.on_disconnect = self._on_disconnect
        self.client.connect(host, port)

        self.is_running = True

    def subscribe_to_topic(self, topic, callback):
        if topic not in set(map(lambda x: x[0], self.on_message_hooks)):
            self.client.subscribe(topic)

        self.on_message_hooks.append((topic, callback))

    def watch_mqtt(self):
        self.client.loop_forever()

    def stop(self):
        self.is_running = False
        self.client.disconnect()

    def catch_sigint(self, signum, frame):
        logging.info('\rInterrupted!')
        logging.info('Stopping...')
        self.stop()

    def publish_persistent(self, topic, payload):
        self.client.publish(topic, payload, retain=True)

    def unsubscribe(self, topic):
        self.client.unsubscribe(topic)

    def _on_connect(self, client, userdata, flags, rc):
        logging.info('MQTT connected!')

    def _on_disconnect(self, client, userdata, rc):
        if self.is_running and rc != mqtt.MQTT_ERR_SUCCESS:
            logging.warning('Lost connection to the MQTT broker!')
        else:
            logging.info('MQTT disconnected!')

    def _on_message(self, client, userdata, message):
        topic = message.topic
        payload = message.payload
        data = None

        try:
            # Check for zlib header and decompress if neccessary
            if payload[0] == 0x78 and payload[1] == 0x9c:
                payload = zlib.decompress(payload)

            # Get data from json
            data = json.loads(payload.decode('utf-8'))
        except Exception as e:
            logging.warning('Could not decode message of length {} in topic {}'.format(len(payload), topic))
            return

        for callback_topic, callback in self.on_message_hooks:
            if topic.startswith(callback_topic):
                callback(topic, data)