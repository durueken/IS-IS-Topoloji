package com.nokia.topology.parser;

/** Bir IS-IS instance içindeki interface'in operasyonel bilgileri. */
public record IsisInterface(
        int instance,
        String name,
        String level,
        int circuitId,
        String operationalState,
        Integer l1Metric,
        Integer l2Metric,
        String type) {
}
