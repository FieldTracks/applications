from datetime import datetime, timezone

class Utils:
    def iso_to_tstamp(iso_time):
        # Parse time string (time is handled in UTC)
        time_iso = iso_time
        time_dt = datetime.strptime(time_iso, '%Y-%m-%dT%H:%M:%SZ')
        time_dt = time_dt.replace(tzinfo=timezone.utc)
        timestamp = int(time_dt.timestamp())
        return timestamp


class BeaconId:
    # Might be used as alternative for
    # beacon identification. Question
    # is if all devices provide a
    # unique, non-changing MAC address

    def __init__(self, uuid, major, minor):
        self.uuid = uuid
        self.major = major
        self.minor = minor

    def __eq__(self, other):
        return (self.uuid, self.major, self.minor) == (other.uuid, other.major, other.minor)

    def __hash__(self):
        return hash((self.uuid, self.major, self.minor))


class Contact:
    def __init__(self, timestamp, mac_address, b_address, rssi_min, rssi_max, rssi_avg, tx_rssi):
        self.timestamp = timestamp
        self.mac_address = mac_address
        self.b_address = b_address
        self.rssi_min = rssi_min
        self.rssi_max = rssi_max
        self.rssi_avg = rssi_avg
        self.tx_rssi = tx_rssi


class Stone:
    def __init__(self, mac_address, b_address, comment):
        # Static data
        self.mac_address = mac_address
        self.b_address = b_address
        self.comment = comment

        # Updates
        self.last_update = ''
        self.contacts = []

    def update(self, iso_time, recent_contacts):
        # We assume for now that updates are fairly recent
        # NOTE: This might change in the future if we
        # queue and send messages via LoRaWAN
        self.last_update = iso_time
        timestamp = Utils.iso_to_tstamp(iso_time)

        # Remove contacts that are older than 60 seconds
        self.contacts = [c for c in self.contacts if Utils.iso_to_tstamp(c.timestamp) >= (timestamp - 60)]

        # Update or add new contacts
        for ct in recent_contacts:
            self.contacts = list(filter(lambda x : x.mac_address != ct.mac_address, self.contacts))
            self.contacts.append(ct)
