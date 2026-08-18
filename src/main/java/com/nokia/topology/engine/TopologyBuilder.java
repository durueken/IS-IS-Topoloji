package com.nokia.topology.engine;

import com.nokia.topology.domain.Device;
import com.nokia.topology.domain.DeviceTier;
import com.nokia.topology.domain.Link;
import com.nokia.topology.domain.LinkCapacity;
import com.nokia.topology.domain.Topology;
import com.nokia.topology.parser.IsisAdjacency;
import com.nokia.topology.parser.IsisInterface;
import com.nokia.topology.parser.ParsedRouter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parser çıktılarından, çıktı katmanından bağımsız Topology grafiği üretir. */
public final class TopologyBuilder {
    private static final Pattern TIER = Pattern.compile("(?i)(?:^|[_-])(t[234]|[hz][34])(?:[_-]|$)");
    private static final Pattern LAG_ID = Pattern.compile("(?i)(?:^|[|_-])lag-(\\d+)(?:[|_-]|$)");
    private static final Pattern CAPACITY = Pattern.compile("(?i)(?:^|[|_-])ge(?:t)?(\\d+)(?:[|_-]|$)");

    public Topology build(Collection<ParsedRouter> routers) {
        Map<String, Device> devices = new LinkedHashMap<>();
        for (ParsedRouter router : routers) {
            if (router.hostname() != null && !router.hostname().isBlank()) {
                devices.putIfAbsent(router.hostname(), createDevice(router.hostname()));
            }
        }

        Map<ConnectionKey, Link> linksByConnection = new LinkedHashMap<>();
        for (ParsedRouter router : routers) {
            if (router.hostname() == null || router.hostname().isBlank()) {
                continue;
            }
            Device source = devices.computeIfAbsent(router.hostname(), this::createDevice);
            for (IsisAdjacency adjacency : router.adjacencies()) {
                if (!adjacency.isUp()) {
                    continue;
                }
                Device target = devices.computeIfAbsent(adjacency.neighborId(), this::createDevice);
                IsisInterface isisInterface = router.findInterface(adjacency.instance(), adjacency.interfaceName()).orElse(null);
                Link candidate = createLink(source, target, adjacency, isisInterface);
                ConnectionKey key = ConnectionKey.from(candidate);
                linksByConnection.merge(key, candidate, this::mergeReciprocalObservations);
            }
        }

        Topology topology = new Topology();
        devices.values().forEach(topology::addDevice);
        linksByConnection.values().forEach(topology::addLink);
        return topology;
    }

    private Link createLink(Device source, Device target, IsisAdjacency adjacency, IsisInterface isisInterface) {
        Integer l1Metric = isisInterface == null ? null : isisInterface.l1Metric();
        Integer l2Metric = isisInterface == null ? null : isisInterface.l2Metric();
        Integer selectedMetric = switch (adjacency.usage()) {
            case "L1" -> l1Metric;
            case "L2" -> l2Metric;
            default -> l2Metric != null ? l2Metric : l1Metric;
        };
        return new Link(source, target, adjacency.interfaceName(), null, selectedMetric,
                adjacency.instance(), adjacency.usage(), l1Metric, l2Metric, capacityFrom(adjacency.interfaceName()));
    }

    private Link mergeReciprocalObservations(Link first, Link second) {
        boolean reverseDirection = first.source().systemId().equals(second.target().systemId())
                && first.target().systemId().equals(second.source().systemId());
        if (!reverseDirection) {
            return first;
        }
        return new Link(first.source(), first.target(), first.sourceInterface(), second.sourceInterface(),
                first.metric(), first.isisInstance(), first.level(), first.l1Metric(), first.l2Metric(), first.capacity());
    }

    private Device createDevice(String hostname) {
        return new Device(hostname, hostname, tierFrom(hostname));
    }

    private DeviceTier tierFrom(String hostname) {
        Matcher match = TIER.matcher(hostname);
        if (!match.find()) {
            return DeviceTier.UNKNOWN;
        }
        return switch (match.group(1).toLowerCase()) {
            case "t2" -> DeviceTier.T2_CORE;
            case "t3", "h3", "z3" -> DeviceTier.T3_AGGREGATION;
            case "t4", "h4", "z4" -> DeviceTier.T4_ACCESS;
            default -> DeviceTier.UNKNOWN;
        };
    }

    private LinkCapacity capacityFrom(String interfaceName) {
        Matcher match = CAPACITY.matcher(interfaceName);
        if (!match.find()) {
            return LinkCapacity.UNKNOWN;
        }
        int gigabits = Integer.parseInt(match.group(1));
        if (gigabits >= 400) {
            return LinkCapacity.G400_PLUS;
        }
        return gigabits >= 100 ? LinkCapacity.G100_TO_399 : LinkCapacity.UNKNOWN;
    }

    private record ConnectionKey(String leftDevice, String rightDevice, Integer instance, String lagOrInterface) {
        static ConnectionKey from(Link link) {
            String left = link.source().systemId();
            String right = link.target().systemId();
            if (left.compareTo(right) > 0) {
                String swap = left;
                left = right;
                right = swap;
            }
            return new ConnectionKey(left, right, link.isisInstance(), stableLinkIdentifier(link.sourceInterface()));
        }

        private static String stableLinkIdentifier(String interfaceName) {
            Matcher lag = LAG_ID.matcher(interfaceName);
            return lag.find() ? "lag-" + lag.group(1) : interfaceName;
        }
    }
}
