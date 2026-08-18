package com.nokia.topology.layout;

import com.nokia.topology.domain.Device;
import com.nokia.topology.domain.DeviceTier;
import com.nokia.topology.domain.Link;
import com.nokia.topology.domain.Topology;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * T2/T3/T4 halkalarını koruyan, kuvvet yönlendirmeli yerleşim algoritması.
 * Bağlantılı düğümler yaklaşırken her düğüm kendi katmanındaki hedef bölgeye bağlı kalır.
 */
public final class HierarchicalLayout {
    private static final double CENTER_X = 1000;
    private static final double CENTER_Y = 750;
    private static final int ITERATIONS = 420;
    private static final double IDEAL_DISTANCE = 240;

    public Map<String, NodePosition> layout(Topology topology) {
        List<Device> devices = new ArrayList<>(topology.devices());
        if (devices.isEmpty()) {
            return Map.of();
        }

        Map<String, Integer> degree = degreeByDevice(topology);
        Map<DeviceTier, List<Device>> groups = groupAndSort(devices, degree);
        Map<String, Vec> homePositions = initialPositions(groups);
        Map<String, Vec> positions = copyPositions(homePositions);

        applyConstrainedForces(topology, devices, positions, homePositions);
        return immutablePositions(positions);
    }

    /** Seyrek, bağımsız bağlantı kümelerini kompakt bir ızgarada paketler. */
    public Map<String, NodePosition> layoutCompact(Topology topology) {
        Map<String, NodePosition> regular = layout(topology);
        Map<String, Vec> positions = new LinkedHashMap<>();
        regular.forEach((id, point) -> positions.put(id, new Vec(point.x(), point.y())));
        packConnectedComponents(topology, positions);
        return immutablePositions(positions);
    }

    private void packConnectedComponents(Topology topology, Map<String, Vec> positions) {
        Map<String, List<String>> neighbors = new HashMap<>();
        topology.devices().forEach(device -> neighbors.put(device.systemId(), new ArrayList<>()));
        for (Link link : topology.links()) {
            neighbors.get(link.source().systemId()).add(link.target().systemId());
            neighbors.get(link.target().systemId()).add(link.source().systemId());
        }

        List<List<String>> components = new ArrayList<>();
        java.util.Set<String> visited = new java.util.HashSet<>();
        for (String start : neighbors.keySet()) {
            if (!visited.add(start)) continue;
            List<String> component = new ArrayList<>();
            ArrayDeque<String> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                String current = queue.removeFirst();
                component.add(current);
                for (String neighbor : neighbors.get(current)) {
                    if (visited.add(neighbor)) queue.addLast(neighbor);
                }
            }
            components.add(component);
        }
        components.sort(Comparator.comparingInt(List<String>::size).reversed());
        // Tek, büyük T2 kümesini sıkıştırmak etiket ve çizgileri üst üste getirir.
        // Bu durumda Fruchterman-Reingold'un ürettiği geniş yerleşim korunur.
        if (components.size() == 1) {
            return;
        }
        int columns = Math.max(1, (int) Math.ceil(Math.sqrt(components.size())));
        for (int index = 0; index < components.size(); index++) {
            List<String> component = components.get(index);
            double centerX = component.stream().mapToDouble(id -> positions.get(id).x).average().orElse(CENTER_X);
            double centerY = component.stream().mapToDouble(id -> positions.get(id).y).average().orElse(CENTER_Y);
            double targetX = 500 + (index % columns) * 900;
            double targetY = 420 + (index / columns) * 700;
            for (String id : component) {
                Vec point = positions.get(id);
                // FR ilişkilerini korurken geniş halka aralıklarını azaltır.
                point.x = targetX + (point.x - centerX) * 0.55;
                point.y = targetY + (point.y - centerY) * 0.55;
            }
        }
    }

    private Map<String, Integer> degreeByDevice(Topology topology) {
        Map<String, Integer> degree = new HashMap<>();
        topology.devices().forEach(device -> degree.put(device.systemId(), 0));
        for (Link link : topology.links()) {
            degree.merge(link.source().systemId(), 1, Integer::sum);
            degree.merge(link.target().systemId(), 1, Integer::sum);
        }
        return degree;
    }

    private Map<DeviceTier, List<Device>> groupAndSort(List<Device> devices, Map<String, Integer> degree) {
        Map<DeviceTier, List<Device>> groups = new EnumMap<>(DeviceTier.class);
        for (DeviceTier tier : DeviceTier.values()) {
            groups.put(tier, new ArrayList<>());
        }
        devices.forEach(device -> groups.get(device.tier()).add(device));
        Comparator<Device> order = Comparator
                .comparingInt((Device device) -> degree.getOrDefault(device.systemId(), 0)).reversed()
                .thenComparing(Device::hostname);
        groups.values().forEach(group -> group.sort(order));
        return groups;
    }

    private Map<String, Vec> initialPositions(Map<DeviceTier, List<Device>> groups) {
        Map<String, Vec> homes = new LinkedHashMap<>();
        placeRing(groups.get(DeviceTier.T2_CORE), 0, homes);
        placeRing(groups.get(DeviceTier.T3_AGGREGATION), 380, homes);
        placeRing(groups.get(DeviceTier.T4_ACCESS), 700, homes);
        placeRing(groups.get(DeviceTier.UNKNOWN), 1020, homes);
        return homes;
    }

    private void placeRing(List<Device> devices, double baseRadius, Map<String, Vec> homes) {
        if (devices.isEmpty()) {
            return;
        }
        if (devices.size() == 1 && baseRadius == 0) {
            homes.put(devices.getFirst().systemId(), new Vec(CENTER_X, CENTER_Y));
            return;
        }
        // Uzun cihaz adları ve etiketler için, düğüm sayısı arttıkça halka genişler.
        double radius = Math.max(baseRadius == 0 ? 180 : baseRadius, devices.size() * 58.0);
        for (int index = 0; index < devices.size(); index++) {
            double angle = -Math.PI / 2 + (2 * Math.PI * index / devices.size());
            homes.put(devices.get(index).systemId(), new Vec(
                    CENTER_X + radius * Math.cos(angle),
                    CENTER_Y + radius * Math.sin(angle)));
        }
    }

    private Map<String, Vec> copyPositions(Map<String, Vec> homes) {
        Map<String, Vec> copy = new LinkedHashMap<>();
        homes.forEach((id, position) -> copy.put(id, new Vec(position.x, position.y)));
        return copy;
    }

    private void applyConstrainedForces(
            Topology topology,
            List<Device> devices,
            Map<String, Vec> positions,
            Map<String, Vec> homes) {
        double temperature = 44;
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Map<String, Vec> displacement = zeroVectors(devices);

            applyRepulsion(devices, positions, displacement);
            applyAttraction(topology.links(), positions, displacement);
            applyLayerAnchors(devices, positions, homes, displacement);

            for (Device device : devices) {
                Vec delta = displacement.get(device.systemId());
                double distance = Math.max(1, delta.length());
                double scale = Math.min(distance, temperature) / distance;
                Vec position = positions.get(device.systemId());
                position.x += delta.x * scale;
                position.y += delta.y * scale;
            }
            temperature *= 0.985;
        }
    }

    private Map<String, Vec> zeroVectors(List<Device> devices) {
        Map<String, Vec> result = new HashMap<>();
        devices.forEach(device -> result.put(device.systemId(), new Vec(0, 0)));
        return result;
    }

    private void applyRepulsion(List<Device> devices, Map<String, Vec> positions, Map<String, Vec> displacement) {
        for (int first = 0; first < devices.size(); first++) {
            for (int second = first + 1; second < devices.size(); second++) {
                String firstId = devices.get(first).systemId();
                String secondId = devices.get(second).systemId();
                Vec difference = positions.get(firstId).minus(positions.get(secondId));
                double distance = Math.max(1, difference.length());
                double force = (IDEAL_DISTANCE * IDEAL_DISTANCE) / distance;
                // Yakın düğümlerde, uzun isim kutularının da çakışmaması için ek itme uygulanır.
                if (distance < 190) {
                    force *= 2.8;
                }
                Vec vector = difference.normalized().times(force);
                displacement.get(firstId).add(vector);
                displacement.get(secondId).add(vector.times(-1));
            }
        }
    }

    private void applyAttraction(Collection<Link> links, Map<String, Vec> positions, Map<String, Vec> displacement) {
        for (Link link : links) {
            String sourceId = link.source().systemId();
            String targetId = link.target().systemId();
            Vec difference = positions.get(sourceId).minus(positions.get(targetId));
            double distance = Math.max(1, difference.length());
            double force = (distance * distance / IDEAL_DISTANCE) * 0.32;
            Vec vector = difference.normalized().times(force);
            displacement.get(sourceId).add(vector.times(-1));
            displacement.get(targetId).add(vector);
        }
    }

    private void applyLayerAnchors(
            List<Device> devices,
            Map<String, Vec> positions,
            Map<String, Vec> homes,
            Map<String, Vec> displacement) {
        for (Device device : devices) {
            String id = device.systemId();
            Vec towardHome = homes.get(id).minus(positions.get(id));
            double strength = device.tier() == DeviceTier.T2_CORE ? 0.085 : 0.040;
            displacement.get(id).add(towardHome.times(strength));
        }
    }

    private Map<String, NodePosition> immutablePositions(Map<String, Vec> positions) {
        Map<String, NodePosition> result = new LinkedHashMap<>();
        positions.forEach((id, position) -> result.put(id, new NodePosition(position.x, position.y)));
        return result;
    }

    private static final class Vec {
        private double x;
        private double y;

        private Vec(double x, double y) {
            this.x = x;
            this.y = y;
        }

        private Vec minus(Vec other) {
            return new Vec(x - other.x, y - other.y);
        }

        private Vec normalized() {
            double length = length();
            return length == 0 ? new Vec(0, 0) : new Vec(x / length, y / length);
        }

        private Vec times(double factor) {
            return new Vec(x * factor, y * factor);
        }

        private void add(Vec other) {
            x += other.x;
            y += other.y;
        }

        private double length() {
            return Math.hypot(x, y);
        }
    }
}
