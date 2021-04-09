import {environment} from "../environments/environment";

export interface AggregatedGraphNew {
  timestmp: string // ISO Timestamp of generation

  // Devices included in the graph. This concerns both jellingstone and non-Jelling-Stone devices
  nodes: AggregatedGraphDevice[]

  // Let's try having one link per device
  // This is the strongest link during the scanning i.e.
  // Effectively, this results in: For each Sensor Contact, take the maximum RSSI over all devices and scans during the interval
  // We want historic data as well:
  // If a stone is offline or a general device is no longer detected, the last links should be in effect for a configurable amount of time
  // Such as 48 hours or so.
  links: AggregatedGraphLink[]

}
export interface AggregatedGraphDevice {
  id: string // UUID-MAJOR-MINOR if present, MAC otherwise
  mac: string // MAC: Mandatory for all devices
  lastseen: string  // When was at seen by the aggregator
  localstone: boolean // True, if a device is a JellingStone device transmitting data to this aggregator
  uuid?: string // UUID, if device is a beacon or stone
  major?: string // major, if device is a beacon or stone
  minor?: string // minor, if device is a beacon or stone
}

export interface AggregatedGraphLink {
  source: string // Id a stone .. but strictly speaking, this is not required as of now.
  target: string // Id target
  timestmp: string // When was the link detected
  rssi: number // RSSI of the link
}

// Simple test-factory for creating a certain test
// Setup: Add ten stones to the graph
// 245 Rounds: Add new Contacts, random assignment.
export class AggregatedGraphNewGenerator {
  private stones: AggregatedGraphDevice[] = []
  private contacts: AggregatedGraphDevice[] = []

  constructor() {
    const now = new Date().toISOString()
    const uuid = environment.uuid
    // Create 10 Stones
    for (let num = 0; num < 10; num++){
      this.stones.push({
        id: `${uuid}-4711-${num}`,
        mac: this.numberToMac(num),
        lastseen: now,
        localstone: true,
        uuid: uuid,
        major: "4711",
        minor: num.toString(),
      })
    }
    // Create 240 BLE beacons
    for (let num = 11; num < 256; num++){
      this.contacts.push({
        id: this.numberToMac(num),
        mac: this.numberToMac(num),
        lastseen: now,
        localstone: false
      })
    }
  }

  graphForIteration(iter: number): AggregatedGraphNew {
    const nodes = this.stones.map( (value => value))
    const links: AggregatedGraphLink[] = []
    const now = new Date().toISOString()
    for(let ctr = 0; ctr < iter && ctr < 244; ctr++){
      const contact = this.contacts[ctr]
      const stone = Math.floor(Math.random() * 10)
      nodes.push(contact)
      links.push({
        source: this.stones[stone].id,
        target: contact.id,
        timestmp: now,
        rssi: -42
      })
    }
    return {
      nodes: nodes,
      links: links,
      timestmp: now
    }
  }

  private numberToMac(num: number): string {
    if(num < 16) {
      return `00:00:00:00:00:0${num.toString(16)}`
    } else {
      return `00:00:00:00:00:${num.toString(16)}`
    }

  }
}
