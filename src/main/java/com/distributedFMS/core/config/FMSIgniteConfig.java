package com.distributedFMS.core.config;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import com.distributedFMS.core.model.Alarm;

import java.util.Arrays;

public class FMSIgniteConfig {
    
    private static final String CLUSTER_NAME = "distributed-fms-cluster";
    private static final String ALARMS_CACHE = "alarms";
    
    public static Ignite createIgniteInstance(String nodeName, boolean clientMode) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        
        cfg.setIgniteInstanceName(nodeName);
        cfg.setClientMode(clientMode);
        
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        
        ipFinder.setAddresses(Arrays.asList(
            "127.0.0.1:47500..47509",
            "127.0.0.1:47510..47519"
        ));
        
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);
        
        if (!clientMode) {
            DataStorageConfiguration storageCfg = new DataStorageConfiguration();
            
            DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
            defaultRegion.setName("Default_Region");
            defaultRegion.setInitialSize(256 * 1024 * 1024);
            defaultRegion.setMaxSize(1024 * 1024 * 1024);
            defaultRegion.setPersistenceEnabled(false);
            
            storageCfg.setDefaultDataRegionConfiguration(defaultRegion);
            cfg.setDataStorageConfiguration(storageCfg);
        }
        
        cfg.setCacheConfiguration(createAlarmCacheConfig());
        
        return Ignition.start(cfg);
    }
    
    private static CacheConfiguration<String, Alarm> createAlarmCacheConfig() {
        CacheConfiguration<String, Alarm> cacheConfig = new CacheConfiguration<>();
        
        cacheConfig.setName(ALARMS_CACHE);
        cacheConfig.setCacheMode(CacheMode.PARTITIONED);
        cacheConfig.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        cacheConfig.setBackups(1);
        
        GeographicAffinityFunction affinityFunction = new GeographicAffinityFunction();
        cacheConfig.setAffinity(affinityFunction);
        
        cacheConfig.setIndexedTypes(String.class, Alarm.class);
        
        return cacheConfig;
    }
    
    public static String getAlarmsCacheName() {
        return ALARMS_CACHE;
    }
}

class GeographicAffinityFunction extends RendezvousAffinityFunction {
    
    public GeographicAffinityFunction() {
        super(false, 32);
    }
    
    @Override
    public int partition(Object key) {
        if (key instanceof Alarm) {
            Alarm alarm = (Alarm) key;
            String region = alarm.getGeographicRegion();
            
            if (region != null) {
                return Math.abs(region.hashCode()) % getPartitions();
            }
        }
        
        return super.partition(key);
    }
}
