use config::{ConfigError, Config, File};
use serde_derive::Deserialize;

#[derive(Debug, Deserialize)]
pub struct Mqtt {
    pub hostname: String,
    pub use_tls: bool,
    pub ca_cert: String,
    pub insecure: bool,
    pub username: String,
    pub password: String
}

#[derive(Debug, Deserialize)]
pub struct Settings {
    pub mqtt: Mqtt
}

impl Settings {
    pub fn new() -> Result<Self, ConfigError> {
        let mut s = Config::new();

        // Start off by merging in the "default" configuration file
        s.merge(File::with_name("Settings"))?;
        s.try_into()
    }
}