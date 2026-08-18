package com.nokia.topology.layout;

import com.nokia.topology.domain.Device;
import com.nokia.topology.domain.DeviceTier;
import com.nokia.topology.domain.Link;
import com.nokia.topology.domain.Topology;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HierarchicalLayoutTest {
    @Test
    void keepsDeviceTiersInIncreasingDistanceFromTheCenter() {
        Device core = new Device("core_t2_1", "core_t2_1", DeviceTier.T2_CORE);
        Device aggregation = new Device("aggregation_t3_1", "aggregation_t3_1", DeviceTier.T3_AGGREGATION);
        Device access = new Device("access_t4_1", "access_t4_1", DeviceTier.T4_ACCESS);
        Topology topology = new Topology();
        topology.addLink(new Link(core, aggregation, null, null, 10));
        topology.addLink(new Link(aggregation, access, null, null, 10));

        Map<String, NodePosition> positions = new HierarchicalLayout().layout(topology);
        double coreDistance = distanceFromCenter(positions.get(core.systemId()));
        double aggregationDistance = distanceFromCenter(positions.get(aggregation.systemId()));
        double accessDistance = distanceFromCenter(positions.get(access.systemId()));

        assertTrue(coreDistance < aggregationDistance);
        assertTrue(aggregationDistance < accessDistance);
    }

    @Test
    void packsDisconnectedComponentsForCompactView() {
        Device first = new Device("first_t2_1", "first_t2_1", DeviceTier.T2_CORE);
        Device second = new Device("second_t3_1", "second_t3_1", DeviceTier.T3_AGGREGATION);
        Device third = new Device("third_t2_1", "third_t2_1", DeviceTier.T2_CORE);
        Device fourth = new Device("fourth_t3_1", "fourth_t3_1", DeviceTier.T3_AGGREGATION);
        Topology topology = new Topology();
        topology.addLink(new Link(first, second, null, null, 10));
        topology.addLink(new Link(third, fourth, null, null, 10));

        Map<String, NodePosition> positions = new HierarchicalLayout().layoutCompact(topology);

        assertTrue(positions.values().stream().allMatch(point -> point.x() > 0 && point.y() > 0));
    }

    private double distanceFromCenter(NodePosition position) {
        return Math.hypot(position.x() - 1000, position.y() - 750);
    }
}
