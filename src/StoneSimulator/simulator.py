#!/usr/bin/env python3

import argparse
import json
import time
import copy
import math
import random
import signal
import sys
import ssl
from datetime import datetime
import paho.mqtt.client as mqtt

# File handles limit
import resource
resource.setrlimit(resource.RLIMIT_NOFILE, (65536, 65536))


JSON_STONE = json.loads("""{
  "uuid": "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00",
  "major": 0,
  "minor": 0,
  "timestamp": "1970-01-01T00:00:00Z",
  "comment": "simulated stone [uninitialized]",
  "data": []
}""")

JSON_STONE_DATA = json.loads("""{
  "mac": "00:00:00:00:00:00",
  "uuid": "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00",
  "major": 0,
  "minor": 0,
  "min": 0,
  "max": 0,
  "avg": 0,
  "remoteRssi": 0
}""")


class Simulator:
    class StoneData:
        def __init__(self, client, mac, template, position):
            self.client = client
            self.mac = mac
            self.template = template
            self.position = position

    def __init__(self, area_width, area_height, interval, stone_num):
        self.width = area_width
        self.height = area_height
        self.interval = interval
        self.stone_num = stone_num

    def connect(self, host, port, cert, insecure, user, passwd, tls):
        self.stones = list()

        for stone_id in range(self.stone_num):
            # Setup connection
            client = mqtt.Client('StoneSim{}'.format(stone_id))
            client.username_pw_set(user, passwd)
            if tls != 0 and cert is not None:
                client.tls_set(cert, tls_version=ssl.PROTOCOL_TLSv1_2)
                if insecure:
                    client.tls_insecure_set(True)
            client.connect(host, port)

            # Generate mac and uuid
            id_hex = stone_id.to_bytes(((stone_id.bit_length() + 7) // 8), "big").hex().rjust(6, '0')
            id_hex_colons = ':'.join(a + b for a, b in zip(id_hex[::2], id_hex[1::2]))
            mac = "ff:ff:ff:" + id_hex_colons
            uuid = "ff:ff:ff:ff:ff:ff:ff:ff:ff:ff:ff:ff:ff:" + id_hex_colons

            # Generate template
            template = copy.deepcopy(JSON_STONE)
            template['uuid'] = uuid
            template['major'] = 99
            template['minor'] = stone_id
            template['comment'] = "simulated stone nr. {}".format(stone_id)

            # Set initial location
            rand_pos_x = random.uniform(0, self.width)
            rand_pos_y = random.uniform(0, self.height)
            position = (rand_pos_x, rand_pos_y)

            self.stones.append(Simulator.StoneData(client, mac, template, position))

    def start_sync(self):
        self.running = True
        self.simulator()

    def close(self):
        self.running = False
        for stone_id in range(self.stone_num):
            self.stones[stone_id].client.disconnect()

    def start(self):
        # Start new thread
        self.running = True
        self.sim_thread = Thread(target=self.simulator)
        self.sim_thread.start()

    def stop(self):
        self.running = False
        self.sim_thread.join()

    def simulator(self):
        round_start_time = time.time()
        step_time = self.interval / self.stone_num
        round_num_failed = 0

        while self.running:
            for stone_id in range(self.stone_num):
                # Publish simulated stone
                stone = copy.deepcopy(self.stones[stone_id].template)
                stone['timestamp'] = datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%SZ')
                stone['data'] = self.simulate_reception(stone_id)
                msg_info = self.stones[stone_id].client.publish('JellingStone/' + self.stones[stone_id].mac, json.dumps(stone).encode('utf-8'))
                if msg_info.rc == mqtt.MQTT_ERR_SUCCESS:
                    msg_info.wait_for_publish()
                else:
                    round_num_failed += 1

                # Sleep until deadline
                deadline = round_start_time + (stone_id + 1) * step_time
                remaining = deadline - time.time()
                if remaining > 0:
                    time.sleep(remaining)

            # TODO: Update stone positions here

            # Stats for this round
            behind = round(time.time() - (round_start_time + self.interval), 2)
            print("Behind by {} seconds, {} failed to send".format(behind, round_num_failed))
            round_start_time += self.interval
            round_num_failed = 0

    def simulate_reception(self, stone_id):
        contacts = list()
        for s in range(self.stone_num):
            if s == stone_id:
                continue
            distance = math.sqrt(math.pow(self.stones[s].position[0] - self.stones[stone_id].position[0], 2) + math.pow(self.stones[s].position[0] - self.stones[stone_id].position[0], 2))

            # TODO: need to implement a correct formula for signal strength here
            expected_strength = 0 - (distance * 3)

            if expected_strength > -50:
                contact = copy.deepcopy(JSON_STONE_DATA)
                contact['mac'] = self.stones[s].mac
                contact['uuid'] = self.stones[s].template['uuid']
                contact['major'] = self.stones[s].template['major']
                contact['minor'] = self.stones[s].template['minor']
                contact['min'] = expected_strength
                contact['max'] = expected_strength
                contact['avg'] = expected_strength
                contacts.append(contact)
        return contacts


def signal_handler(signal, frame):
    print("\rStopping...")
    sim.close()


if __name__ == '__main__':
    # Parse command line parameters
    parser = argparse.ArgumentParser(description='Simulates stone data for testing')
    parser.add_argument('-H', '--host', help='specify a hostname or ip address', default='localhost')
    parser.add_argument('-p', '--port', help='specify a custom port', type=int, default=1883)
    parser.add_argument('-c', '--cert', help='specify a server certificate')
    parser.add_argument('-I', '--insecure', help='Don\'t check hostname in certificate', action='store_true')
    parser.add_argument('-u', '--user', help='specify a username', default='stonesimulator')
    parser.add_argument('-P', '--passwd', help='specify password', default='')
    parser.add_argument('-s', '--size', help='size of simulated area: <size> x <size> m^2', type=int, default=200)
    parser.add_argument('-i', '--interval', help='interval in which each stone sends data', type=int, default=8)
    parser.add_argument('-n', '--stones', help='number of stones to simulate', type=int, default=20)
    parser.add_argument('-t', '--tls', help='Use TLS', type=int, default=1)
    args = parser.parse_args()

    # Setup simulation
    print("Setting up simulation...")
    sim = Simulator(args.size, args.size, args.interval, args.stones)
    sim.connect(args.host, args.port, args.cert, args.insecure, args.user, args.passwd, args.tls)
    time.sleep(2)
    signal.signal(signal.SIGINT, signal_handler)
    print("Starting simulation...")
    sim.start_sync()

    # Exit
    print("Done!")
    sys.exit(0)
