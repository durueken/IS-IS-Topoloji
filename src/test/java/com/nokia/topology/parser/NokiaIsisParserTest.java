package com.nokia.topology.parser;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NokiaIsisParserTest {
    @Test
    void parsesInstancesWrappedInterfacesAndMetrics() throws IOException {
        String output;
        try (var stream = getClass().getResourceAsStream("/fixtures/masked-r1-isis.txt")) {
            output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        ParsedRouter router = new NokiaIsisParser().parse(output);

        assertEquals("R1", router.hostname());
        assertEquals(5, router.adjacencies().size());
        assertEquals("xxxx-03-1|ge10|lag-190|xxxx-02_2", router.adjacencies().get(1).interfaceName());
        assertEquals(2, router.adjacencies().stream().filter(item -> item.neighborId().equals("R6")).count());
        assertEquals(50, router.findInterface(0, "xxxx-3|ge100|lag-184|xxxx-1").orElseThrow().l2Metric());
        assertNull(router.findInterface(0, "xxxx-3|ge100|lag-184|xxxx-1").orElseThrow().l1Metric());
        assertEquals(40000, router.findInterface(6, "xxxx-1|ge10|lag-195|xxxx-14").orElseThrow().l1Metric());
        assertFalse(router.interfaces().stream().anyMatch(item -> item.instance() == 8));
        assertTrue(router.warnings().isEmpty());
    }

    @Test
    void keepsDeviceWhenOnlyInterfaceOutputIsAvailable() {
        String interfaceOnly = """
                *A:R-INTERFACE-ONLY# show router isis all interface
                Rtr Base ISIS Instance 6 Interfaces
                Interface                        Level CircID  Oper      L1/L2 Metric     Type
                -------------------------------------------------------------------------------
                uplink|ge100|lag-10|core         L2    10      Up        -/50             p2p
                -------------------------------------------------------------------------------
                Interfaces : 1
                """;

        ParsedRouter router = new NokiaIsisParser().parse(interfaceOnly);

        assertEquals("R-INTERFACE-ONLY", router.hostname());
        assertEquals(0, router.adjacencies().size());
        assertEquals(1, router.interfaces().size());
    }

    @Test
    void joinsAWrappedAdjacencyInterfaceNameWithoutAddingWhitespace() {
        String adjacencyOnly = """
                *A:18_sitea_t3_1# show router isis all adjacency
                Rtr Base ISIS Instance 0 Adjacency
                System ID                Usage State Hold Interface                     MT-ID
                -------------------------------------------------------------------------------
                55_siteb_sr12e_t2_2       L2    Up    26   sitea-t3-1|ge10|lag-190|siteb- 0
                                                       t2_2
                -------------------------------------------------------------------------------
                Adjacencies : 1
                """;

        ParsedRouter router = new NokiaIsisParser().parse(adjacencyOnly);

        assertEquals(1, router.adjacencies().size());
        assertEquals("sitea-t3-1|ge10|lag-190|siteb-t2_2",
                router.adjacencies().getFirst().interfaceName());
    }
}
