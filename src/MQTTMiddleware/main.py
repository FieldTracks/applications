import configparser
import logging
import signal
import sys

from mqttservice import MQTTService
from aggregator import Aggregator

MQTT = None

def main():
    global MQTT
    if len(sys.argv) != 2:
        print('Usage: {} <config file>'.format(sys.argv[0]))
        exit(1)

    # Won't check for now if the config file is valid
    # Falls back to default values if options are missing
    config = configparser.ConfigParser()
    config.read(sys.argv[1])

    # Configure logging
    logging.basicConfig(
        format='%(asctime)s.%(msecs)03d %(levelname)s %(name)s %(message)s',
        level=logging.NOTSET,
        datefmt='%s %Y-%m-%d %H:%M:%S'
    )

    # Setup the MQTT service for communication
    logging.info('Starting MQTT service...')
    MQTT = MQTTService(config)
    aggregator = Aggregator(config, MQTT)


    # Catch sigints
    signal.signal(signal.SIGINT, MQTT.catch_sigint)

    # Listen for incoming mqtt data (blocking)
    logging.info('Running...')
    MQTT.watch_mqtt()
    logging.info('Done!')

if __name__ == '__main__':
    main()
