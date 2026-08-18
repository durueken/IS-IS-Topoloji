package com.nokia.topology.parser;

/** Bir cihazın tek IS-IS komşuluk gözlemi. */
public record IsisAdjacency(
        int instance,
        String neighborId,
        String usage,
        String state,
        int holdTimeSeconds,
        String interfaceName,
        int mtId) {

    public boolean isUp() {
        return "Up".equalsIgnoreCase(state);
    }
}
