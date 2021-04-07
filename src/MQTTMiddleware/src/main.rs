extern crate config;
extern crate paho_mqtt as mqtt;

mod settings;
mod mqttservice;

fn main() {
    let config = settings::Settings::new().unwrap();
    let mut mqtt = mqttservice::init(config);

    
    println!("Start consuming messages...");
    let rx = mqtt.start_consuming();
    for msg in rx.iter() {
        if let Some(msg) = msg {
            // process message
        } else if mqtt.is_connected() ||
                !mqttservice::try_reconnect(&mqtt) {
            break;
        }
    }
}
