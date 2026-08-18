package com.nokia.topology.engine;

import com.nokia.topology.domain.DeviceTier;
import com.nokia.topology.domain.LinkCapacity;
import com.nokia.topology.domain.Topology;
import com.nokia.topology.parser.IsisAdjacency;
import com.nokia.topology.parser.IsisInterface;
import com.nokia.topology.parser.ParsedRouter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TopologyBuilderTest {
    @Test
    void buildsOneBidirectionalLinkAndPreservesParallelLag() {
        ParsedRouter coreOne = new ParsedRouter("site_t2_1",
                List.of(
                        new IsisAdjacency(0, "site_t2_2", "L2", "Up", 20, "a|ge100|lag-10|b", 0),
                        new IsisAdjacency(0, "site_t2_2", "L2", "Up", 20, "a|ge400|lag-11|b", 0)),
                List.of(
                        new IsisInterface(0, "a|ge100|lag-10|b", "L2", 10, "Up", null, 50, "p2p"),
                        new IsisInterface(0, "a|ge400|lag-11|b", "L2", 11, "Up", null, 60, "p2p")),
                List.of());
        ParsedRouter coreTwo = new ParsedRouter("site_t2_2",
                List.of(new IsisAdjacency(0, "site_t2_1", "L2", "Up", 20, "b|ge100|lag-10|a", 0)),
                List.of(new IsisInterface(0, "b|ge100|lag-10|a", "L2", 12, "Up", null, 50, "p2p")),
                List.of());

        Topology topology = new TopologyBuilder().build(List.of(coreOne, coreTwo));

        assertEquals(2, topology.devices().size());
        assertEquals(2, topology.links().size());
        assertEquals(DeviceTier.T2_CORE, topology.devices().stream().findFirst().orElseThrow().tier());
        assertEquals(LinkCapacity.G100_TO_399, topology.links().stream()
                .filter(link -> link.capacity() == LinkCapacity.G100_TO_399).findFirst().orElseThrow().capacity());
        assertEquals(50, topology.links().stream()
                .filter(link -> link.capacity() == LinkCapacity.G100_TO_399).findFirst().orElseThrow().metric());
        assertEquals("b|ge100|lag-10|a", topology.links().stream()
                .filter(link -> link.capacity() == LinkCapacity.G100_TO_399).findFirst().orElseThrow().targetInterface());
    }

    @Test
    void recognizesHAndZDeviceFamiliesAsTheirEquivalentTiers() {
        ParsedRouter h3 = new ParsedRouter("site_h3_1", List.of(), List.of(), List.of());
        ParsedRouter z4 = new ParsedRouter("site_z4_1", List.of(), List.of(), List.of());

        Topology topology = new TopologyBuilder().build(List.of(h3, z4));

        assertEquals(DeviceTier.T3_AGGREGATION, topology.devices().stream()
                .filter(device -> device.hostname().equals("site_h3_1")).findFirst().orElseThrow().tier());
        assertEquals(DeviceTier.T4_ACCESS, topology.devices().stream()
                .filter(device -> device.hostname().equals("site_z4_1")).findFirst().orElseThrow().tier());
    }
}
