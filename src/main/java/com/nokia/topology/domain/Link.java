package com.nokia.topology.domain;

import java.util.Objects;

/** İki router arasındaki IS-IS komşuluğu. */
public record Link(
        Device source,
        Device target,
        String sourceInterface,
        String targetInterface,
        Integer metric,
        Integer isisInstance,
        String level,
        Integer l1Metric,
        Integer l2Metric,
        LinkCapacity capacity) {

    /** Önceki basit kullanım biçimini korur. */
    public Link(Device source, Device target, String sourceInterface, String targetInterface, Integer metric) {
        this(source, target, sourceInterface, targetInterface, metric, null, null, null, null, LinkCapacity.UNKNOWN);
    }

    public Link {
        source = Objects.requireNonNull(source, "Kaynak cihaz zorunludur");
        target = Objects.requireNonNull(target, "Hedef cihaz zorunludur");
        if (source.systemId().equals(target.systemId())) {
            throw new IllegalArgumentException("Bir cihaz kendisine bağlanamaz");
        }
    }
}
