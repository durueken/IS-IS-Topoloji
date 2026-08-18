package com.nokia.topology.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopologyCliTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsDrawioFileFromCliOutput() throws IOException {
        Path input = temporaryDirectory.resolve("r1.txt");
        try (var resource = getClass().getResourceAsStream("/fixtures/masked-r1-isis.txt")) {
            Files.write(input, resource.readAllBytes());
        }
        Path output = temporaryDirectory.resolve("topology.drawio");

        TopologyCli.Result result = TopologyCli.generate(input, output);
        String drawio = Files.readString(output);

        assertEquals(5, result.deviceCount());
        assertEquals(5, result.linkCount());
        assertTrue(drawio.contains("shape=rectangle"));
        assertFalse(drawio.contains("IST-CORE-01"));
        assertFalse(Files.exists(temporaryDirectory.resolve("t2-topology.drawio")));
    }
}
