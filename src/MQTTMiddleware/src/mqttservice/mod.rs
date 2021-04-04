use std::env;
use mqtt::AsyncClient;

use crate::settings::Settings;
use std::{thread, time::Duration};

extern crate paho_mqtt as mqtt;

pub fn try_reconnect(cli: &mqtt::AsyncClient) -> bool
{
    println!("Connection lost. Waiting to retry connection");
    for _ in 0..12 {
        thread::sleep(Duration::from_millis(5000));
        if cli.reconnect().wait().is_ok() {
            println!("Successfully reconnected");
            return true;
        }
    }
    println!("Unable to reconnect after several attempts.");
    false
}

pub fn init(config: Settings) -> AsyncClient {
    let cli = mqtt::CreateOptionsBuilder::new()
                    .server_uri(&config.mqtt.hostname)
                    .client_id("mqttmiddleware")
                    .max_buffered_messages(100)
                    .create_client().unwrap();

    let mut conn_opts = mqtt::ConnectOptionsBuilder::new();
    conn_opts.user_name(config.mqtt.username);
    conn_opts.password(config.mqtt.password);

    if config.mqtt.use_tls {
        let mut trust_store = env::current_dir().unwrap();
        trust_store.push(config.mqtt.ca_cert);

        let ssl_opts = mqtt::SslOptionsBuilder::new()
        .trust_store(trust_store).unwrap()
        .finalize();

        conn_opts.ssl_options(ssl_opts);
    }

    let conn_opts = conn_opts.finalize();

    println!("Connecting to MQTT server...");
    if let Err(_) = cli.connect(conn_opts).wait() {
        panic!("Cannot connect to MQTT server");
    }
    
    cli
}