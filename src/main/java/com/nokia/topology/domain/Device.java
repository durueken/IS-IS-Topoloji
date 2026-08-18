package com.nokia.topology.domain;

import java.util.Objects;

/*Router kimliği. */
public record Device(String hostname, String systemId, DeviceTier tier) {
    public Device {
        if (hostname == null || hostname.isBlank()) {
            throw new IllegalArgumentException("Hostname boş olamaz");
        }
        if (systemId == null || systemId.isBlank()) {
            throw new IllegalArgumentException("System ID boş olamaz");
        }
        tier = Objects.requireNonNullElse(tier, DeviceTier.UNKNOWN);
    }
}
