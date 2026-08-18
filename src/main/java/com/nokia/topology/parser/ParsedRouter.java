package com.nokia.topology.parser;

import java.util.List;
import java.util.Optional;

/** Tek bir router'ın adjacency ve interface çıktılarının parse edilmiş hali. */
public record ParsedRouter(
        String hostname,
        List<IsisAdjacency> adjacencies,
        List<IsisInterface> interfaces,
        List<String> warnings) {

    public ParsedRouter {
        adjacencies = List.copyOf(adjacencies);
        interfaces = List.copyOf(interfaces);
        warnings = List.copyOf(warnings);
    }

    public Optional<IsisInterface> findInterface(int instance, String name) {
        return interfaces.stream()
                .filter(item -> item.instance() == instance && item.name().equals(name))
                .findFirst();
    }
}
