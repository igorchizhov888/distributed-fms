package com.distributedFMS.core.config;

import com.distributedFMS.core.model.Alarm;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import com.distributedFMS.correlation.model.CorrelatedAlarm;
import com.distributedFMS.correlation.config.CorrelationConfig;

import java.util.Arrays;

public class FMSIgniteConfig {

    private static final String CLUSTER_NAME = "distributed-fms-cluster";
    private static final String ALARMS_CACHE = "alarms";

    private static volatile Ignite ignite;

    /**
     * Returns a singleton instance of Ignite.
     * This method uses double-checked locking to ensure thread-safe, lazy initialization.
     */
    public static Ignite getInstance() {
        if (ignite == null) {
            synchronized (FMSIgniteConfig.class) {
                if (ignite == null) {
                    ignite = Ignition.start(createIgniteConfiguration());
                }
            }
        }
        return ignite;
    }

    /**
     * Creates the Ignite configuration for a server node.
     */
    private static IgniteConfiguration createIgniteConfiguration() {
        IgniteConfiguration cfg = new IgniteConfiguration();

        // Use a consistent instance name for the singleton
        cfg.setIgniteInstanceName("singleton-fms-node");
        cfg.setClientMode(false); // This instance is always a server node

        // Discovery SPI
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        // Data Storage Configuration (for server nodes)
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setName("Default_Region");
        defaultRegion.setInitialSize(256 * 1024 * 1024);
        defaultRegion.setMaxSize(1024 * 1024 * 1024);
        defaultRegion.setPersistenceEnabled(false); // In-memory for now
        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);
        cfg.setDataStorageConfiguration(storageCfg);

        // Cache Configuration
        
        cfg.setCacheConfiguration(
            createAlarmCacheConfig(),
            CorrelationConfig.correlatedAlarmsCacheConfig()
        );

        return cfg;
    }

    private static CacheConfiguration<String, Alarm> createAlarmCacheConfig() {
        CacheConfiguration<String, Alarm> cacheConfig = new CacheConfiguration<>();
        cacheConfig.setName(ALARMS_CACHE);
        cacheConfig.setCacheMode(CacheMode.PARTITIONED);
        cacheConfig.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        cacheConfig.setBackups(1);
        cacheConfig.setIndexedTypes(String.class, Alarm.class);
        return cacheConfig;
    }

    public static String getAlarmsCacheName() {
        return ALARMS_CACHE;
    }

    /**
     * Stops the singleton Ignite instance.
     * Useful for cleanup in test environments.
     */
    public static void stopInstance() {
        synchronized (FMSIgniteConfig.class) {
            if (ignite != null) {
                ignite.close();
                ignite = null;
            }
        }
    }
}
