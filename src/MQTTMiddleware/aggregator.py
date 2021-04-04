import datetime
import json
import logging
import threading
import time

from sqlalchemy import (Column, DateTime, ForeignKey, Integer, String,
                        create_engine)
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship, sessionmaker

from models import Utils, BeaconId, Stone, Contact


class World:
    def __init__(self):
        self.stones = {} # Contains stones: mac => stone
        self.descs = {} # Contains descriptions for nodes: mac => (name, color)
        self.lock = threading.Lock()

    def get_lock(self):
        return self.lock

    def get_stones(self):
        return self.stones

    def get_descs(self):
        return self.descs

    def update_stone(self, stone):
        with self.get_lock():
            if stone.mac_address not in self.stones:
                self.stones[stone.mac_address] = stone
            else:
                self.stones[stone.mac_address].update(stone.last_update, stone.contacts)

    def update_desc(self, mac_address, name, color):
        with self.get_lock():
            self.descs[mac_address] = (name, color)


class Aggregator:
    def __init__(self, config, mqttservice):
        self.config = config
        self.world = World()
        if self.config.getboolean('Database', 'EnableLogging', fallback=False):
            logging.info('Setting up database...')
            self.dbs = DBService(self.config)
        else:
            self.dbs = None

        self.channel_in_sensors_prefix = self.config.get('MQTT Channels', 'ChannelPrefixSensors', fallback='JellingStone/')
        self.channel_in_sensors = self.channel_in_sensors_prefix + '+'
        self.channel_in_nameupdate = self.config.get('MQTT Channels', 'ChannelNameUpdates', fallback='NameUpdate')
        self.channel_out_stones = self.config.get('MQTT Channels', 'ChannelStoneInfo', fallback='Aggregated/Stones')
        self.channel_out_graph = self.config.get('MQTT Channels', 'ChannelGraphInfo', fallback='Aggregated/Graph')
        self.channel_out_names = self.config.get('MQTT Channels', 'ChannelNames', fallback='Aggregated/Names')

        self.mqttservice = mqttservice
        self.mqttservice.subscribe_to_topic(self.channel_in_sensors_prefix, self.senors_callback)
        self.mqttservice.subscribe_to_topic(self.channel_in_nameupdate, self.nameupdate_callback)
        self.mqttservice.subscribe_to_topic(self.channel_out_names, self.names_update_callback)

    def aggregate_stones(self, stones):
        # Create list of stones
        stones_info = dict()
        for mac, s in stones.items():
            stones_info[mac] = {'uuid': s.b_address.uuid, 'major': s.b_address.major, 'minor': s.b_address.minor, 'comment': s.comment, 'timestamp': s.last_update}
            if self.config.getboolean('Aggregator', 'StoneInfoIncludeContacts', fallback=True):
                stones_info[mac]['contacts'] = list()
                for c in s.contacts:
                    stones_info[mac]['contacts'].append({'mac': c.mac_address, 'uuid': c.b_address.uuid, 'major': c.b_address.major, 'minor': c.b_address.minor, 'rssi_avg': c.rssi_avg, 'rssi_tx': c.tx_rssi})
        return json.dumps(stones_info)

    def aggregate_graph(self, stones, current_time):
        # Create list of stones
        stones_info = dict()
        for mac, s in stones.items():
            stones_info[mac] = {'uuid': s.b_address.uuid, 'major': s.b_address.major, 'minor': s.b_address.minor, 'comment': s.comment, 'age': current_time - Utils.iso_to_tstamp(s.last_update), 'contacts': []}
            for c in s.contacts:
                stones_info[mac]['contacts'].append({'mac': c.mac_address, 'uuid': c.b_address.uuid, 'major': c.b_address.major, 'minor': c.b_address.minor, 'age': current_time - Utils.iso_to_tstamp(c.timestamp), 'rssi_avg': c.rssi_avg, 'rssi_tx': c.tx_rssi})
        return json.dumps(stones_info)

    def aggregate_descs(self, descriptions):
        # Create list of descriptions
        descs_info = dict()
        for mac in descriptions:
            descs_info[mac] = {'name': descriptions[mac][0], 'color': descriptions[mac][1]}
        return json.dumps(descs_info)

    def senors_callback(self, topic, data):
        # Parse data into Stone object
        mac_address = topic[len(self.channel_in_sensors_prefix):]
        stone = Stone(mac_address, BeaconId(data['uuid'], data['major'], data['minor']), data['comment'])

        # Add contacts
        contacts = list()
        for ct in data['data']:
            bid = BeaconId(ct['uuid'], ct['major'], ct['minor']) if ('uuid' in ct and 'major' in ct and 'minor' in ct) else BeaconId('', 0, 0)
            contacts.append(Contact(data['timestamp'], ct['mac'], bid, ct['min'], ct['max'], ct['avg'], ct['remoteRssi']))
        stone.update(data['timestamp'], contacts)

        # Update world model
        self.world.update_stone(stone)

        # Publish aggregated data
        if(time.time() - self.last_stone_update) >= self.update_interval:
            self.last_stone_update = time.time()
            with self.world.get_lock():
                agg_stones = self.aggregate_stones(self.world.get_stones())
                agg_graph = self.aggregate_graph(self.world.get_stones(), Utils.iso_to_tstamp(data['timestamp']))
            self.mqttservice.publish_persistent(self.channel_out_stones, agg_stones.encode('utf-8'))
            self.mqttservice.publish_persistent(self.channel_out_graph, agg_graph.encode('utf-8'))

        # Store stone event in database
        if self.dbs is not None:
            self.dbs.store_event(stone)

    def nameupdate_callback(self, topic, data):
        # Update the description
        self.world.update_desc(data['mac'], data['name'], data['color'])

        # Compose and pin a new message with all names
        with self.world.get_lock():
            agg_descs = self.aggregate_descs(self.world.get_descs())
        self.mqttservice.publish_persistent(self.channel_out_names, agg_descs.encode('utf-8'))

    def names_update_callback(self, topic, data):
        # This should pick up the last retained aggregated names on startup
        # Since we only need the last message we should unsubscribe here
        self.mqttservice.unsubscribe(self.channel_out_names)

        if type(data) is not dict:
            logging.warning('Can\'t parse retained names!')
            return

        num_imported = 0

        for mac, entry in data.items():
            if type(entry) is dict and 'name' in entry and 'color' in entry:
                self.world.update_desc(mac, entry['name'], entry['color'])
                num_imported += 1
            else:
                logging.warning('Invalid name entry in retained message!')

        logging.info('Imported {} descriptions from last aggregated message'.format(num_imported))

class DBService:
    Base = declarative_base()

    class SensorContact(Base):
        __tablename__ = 'sensor_contacts'

        id = Column(Integer, primary_key=True, autoincrement=True)
        stone_event_id = Column(Integer, ForeignKey('stone_events.id'))
        mac = Column(String(17))
        uuid = Column(String(47))
        major = Column(Integer)
        minor = Column(Integer)
        min = Column(Integer)
        max = Column(Integer)
        avg = Column(Integer)
        remote_rssi = Column(Integer)

        stone_event = relationship('StoneEvent', back_populates='contacts')

    class StoneEvent(Base):
        __tablename__ = 'stone_events'

        id = Column(Integer, primary_key=True, autoincrement=True)
        mac = Column(String(17))
        uuid = Column(String(47))
        major = Column(Integer)
        minor = Column(Integer)
        timestamp = Column(DateTime)
        comment = Column(String(128))

        contacts = relationship('SensorContact', back_populates='stone_event')

    def __init__(self, config):
        host = config.get('Database', 'Hostname', fallback='localhost')
        port = config.getint('Database', 'Port', fallback=3306)
        user = config.get('Database', 'Username', fallback='aggregator')
        passwd = config.get('Database', 'Password', fallback='')
        db = config.get('Database', 'Database', fallback='fieldtracks')

        self.engine = create_engine('mysql+mysqldb://{}:{}@{}:{}/{}'.format(user, passwd, host, port, db))
        DBService.Base.metadata.create_all(self.engine)
        Session = sessionmaker(bind=self.engine)
        self.session = Session()

    def stop(self):
        self.session.close()

    def store_event(self, stone):
        time = datetime.utcfromtimestamp(stone.last_update)
        db_stone = DBService.StoneEvent(mac=stone.mac_address,
                                        uuid=stone.b_address.uuid,
                                        major=stone.b_address.major,
                                        minor=stone.b_address.minor,
                                        timestamp=time,
                                        comment=stone.comment)
        for contact in stone.contacts:
            db_contact = DBService.SensorContact(mac=contact.mac_address,
                                                 uuid=contact.b_address.uuid,
                                                 major=contact.b_address.major,
                                                 minor=contact.b_address.minor,
                                                 min=contact.rssi_min,
                                                 max=contact.rssi_max,
                                                 avg=contact.rssi_avg,
                                                 remote_rssi=contact.tx_rssi)
            db_stone.contacts.append(db_contact)

        self.session.add(db_stone)
        self.session.commit()
