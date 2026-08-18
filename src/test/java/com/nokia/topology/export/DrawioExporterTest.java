package com.nokia.topology.export;

import com.nokia.topology.domain.Device;
import com.nokia.topology.domain.DeviceTier;
import com.nokia.topology.domain.Link;
import com.nokia.topology.domain.LinkCapacity;
import com.nokia.topology.domain.Topology;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawioExporterTest {
    @Test
    void exportsDevicesAndIsisLink() {
        Device core = new Device("IST-CORE-01", "0000.0000.0001", DeviceTier.T2_CORE);
        Device aggregation = new Device("IST-AGG-01", "0000.0000.0002", DeviceTier.T3_AGGREGATION);
        Topology topology = new Topology();
        topology.addLink(new Link(core, aggregation, "to-agg-01", "to-core-01", 10));

        String xml = new DrawioExporter().export(topology);

        assertTrue(xml.contains("IST-CORE-01"));
        assertTrue(xml.contains("IST-AGG-01"));
        assertTrue(xml.contains("IS-IS(I:-.-.M:10)"));
        assertTrue(xml.contains("to-agg-01"));
        assertTrue(xml.contains("to-core-01"));
        assertTrue(xml.contains("x=\"-0.35\""));
        assertTrue(xml.contains("x=\"0.35\""));
        assertTrue(xml.contains("fontSize=7"));
        assertTrue(!xml.contains("diagram name=\"Interface Detayları\""));
    }

    @Test
    void keepsT2ToT2LinksGrayInMainTopology() {
        Device firstCore = new Device("CORE-T2-01", "0000.0000.0001", DeviceTier.T2_CORE);
        Device secondCore = new Device("CORE-T2-02", "0000.0000.0002", DeviceTier.T2_CORE);
        Topology topology = new Topology();
        topology.addLink(new Link(firstCore, secondCore, "ge100", "ge100", 10,
                0, "L2", null, 10, LinkCapacity.G100_TO_399));

        String xml = new DrawioExporter().export(topology);

        assertTrue(xml.contains("strokeColor=#666666"));
        assertTrue(!xml.contains("strokeColor=#FFD700"));
        assertTrue(xml.contains("ge100"));
        assertTrue(xml.contains("IS-IS(I:0.L2.M:10)"));
    }

    @Test
    void usesOneCompactLabelForNonCoreLinks() {
        Device aggregation = new Device("AGG-T3-01", "0000.0000.0003", DeviceTier.T3_AGGREGATION);
        Device access = new Device("ACCESS-T4-01", "0000.0000.0004", DeviceTier.T4_ACCESS);
        Topology topology = new Topology();
        topology.addLink(new Link(aggregation, access, "long-interface-name", null, 700,
                29, "L1", 700, null, LinkCapacity.UNKNOWN));

        String xml = new DrawioExporter().export(topology);

        assertTrue(xml.contains("IS-IS(I:29.L1.M:700)"));
        assertTrue(xml.contains("long-interface-name"));
        assertTrue(xml.contains("edgeStyle=none"));
    }

    @Test
    void usesTierSpecificShapesWithoutExtraInterfaceDetailsDiagram() {
        Device core = new Device("LONG-CORE-T2-DEVICE-NAME", "LONG-CORE-T2-DEVICE-NAME", DeviceTier.T2_CORE);
        Device aggregation = new Device("AGG-T3-01", "0000.0000.0003", DeviceTier.T3_AGGREGATION);
        Device access = new Device("ACCESS-T4-01", "0000.0000.0004", DeviceTier.T4_ACCESS);
        Topology topology = new Topology();
        topology.addLink(new Link(core, aggregation, null, null, null));
        topology.addLink(new Link(aggregation, access, null, null, null));

        String xml = new DrawioExporter().export(topology);

        assertTrue(xml.contains("shape=hexagon"));
        assertTrue(xml.contains("rounded=1"));
        assertTrue(xml.contains("shape=ellipse"));
        assertTrue(xml.contains("width=\"222.0\""));
        assertTrue(!xml.contains("diagram name=\"Interface Detayları\""));
        assertTrue(xml.contains("LONG-CORE-T2-DEVICE-NAME"));
    }
}
