package com.ydlclass.config;

import com.ydlclass.compress.Compressor;
import com.ydlclass.compress.CompressorFactory;
import com.ydlclass.loadbalancer.LoadBalancer;
import com.ydlclass.serialize.Serializer;
import com.ydlclass.serialize.SerializerFactory;
import com.ydlclass.spi.SpiHandler;

import java.util.List;

public class SpiResolver {

    public void loadFromSpi(Configuration configuration) {

        List<ObjectWrapper<LoadBalancer>> loadBalancerWrappers = SpiHandler.getList(LoadBalancer.class);

        if (loadBalancerWrappers != null && loadBalancerWrappers.size() > 0) {
            configuration.setLoadBalancer(loadBalancerWrappers.get(0).getImpl());
        }

        List<ObjectWrapper<Compressor>> objectWrappers = SpiHandler.getList(Compressor.class);
        if (objectWrappers != null) {
            objectWrappers.forEach(CompressorFactory::addCompressor);
        }

        List<ObjectWrapper<Serializer>> serializerObjectWrappers = SpiHandler.getList(Serializer.class);
        if (serializerObjectWrappers != null) {
            serializerObjectWrappers.forEach(SerializerFactory::addSerializer);
        }
    }
}
