pub mod provider;
pub mod provider_registry;

pub mod bigquery;
pub mod clickhouse;
pub mod databricks;
pub mod snowflake;
// Never registered — a test fixture, not a served provider.
#[cfg(test)]
pub mod provider_stub;

pub use provider::{Advertised, ConnectionParams, DbProvider};
pub use provider_registry::ProviderRegistry;

use crate::providers::bigquery::BigQueryProvider;
use crate::providers::clickhouse::ClickhouseProvider;
use crate::providers::databricks::DatabricksProvider;
use crate::providers::snowflake::SnowflakeProvider;

/// Every provider the binary can serve. Whether one is actually served is its own
/// `Advertised::ADVERTISED` const, so enabling or disabling a provider never touches
/// this list.
pub fn build_registry() -> ProviderRegistry {
    let mut registry = ProviderRegistry::new();
    registry.register_probed(DatabricksProvider::new());
    registry.register_probed(SnowflakeProvider::new());
    registry.register_probed(ClickhouseProvider::new());
    registry.register_probed(BigQueryProvider::new());
    registry
}

/// Loads every provider's driver, advertised or not, and reports each on stdout/stderr.
/// Backs `--probe-drivers`, which proves a built image can load all the drivers it ships.
pub fn probe_all() -> bool {
    let providers: [Box<dyn DbProvider>; 4] = [
        Box::new(DatabricksProvider::new()),
        Box::new(SnowflakeProvider::new()),
        Box::new(ClickhouseProvider::new()),
        Box::new(BigQueryProvider::new()),
    ];
    let mut all_loaded = true;
    for provider in &providers {
        match provider.probe() {
            Ok(()) => println!("{}: driver loaded", provider.name()),
            Err(e) => {
                eprintln!("{}: {:#}", provider.name(), e);
                all_loaded = false;
            }
        }
    }
    all_loaded
}
