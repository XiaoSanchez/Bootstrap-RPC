package com.ydlclass.loadbalancer;

import java.net.InetSocketAddress;

public interface Selector {

    InetSocketAddress getNext();

}
