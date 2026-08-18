package com.nokia.topology.domain;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Parser ve çıktı katmanlarından bağımsız ağ grafiği. */
public final class Topology {
    private final Map<String, Device> devicesBySystemId = new LinkedHashMap<>();
    private final Set<Link> links = new LinkedHashSet<>();

    public void addDevice(Device device) {
        devicesBySystemId.putIfAbsent(device.systemId(), device);
    }

    public void addLink(Link link) {
        addDevice(link.source());
        addDevice(link.target());
        links.add(link);
    }

    public Collection<Device> devices() {
        return devicesBySystemId.values();
    }

    public Collection<Link> links() {
        return Set.copyOf(links);
    }
}
