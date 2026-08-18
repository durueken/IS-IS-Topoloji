package com.nokia.topology.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nokia SR OS'un {@code show router isis all adjacency} ve
 * {@code show router isis all interface} çıktılarını ayrıştırır.
 *
 * <p>Parser cihaz veya şehir ismine bağlı değildir; yalnızca SR OS tablo
 * başlıkları ve sütun düzenini kullanır.</p>
 */
public final class NokiaIsisParser {
    private static final Pattern PROMPT = Pattern.compile(
            "^\\*?A:([^#]+)#\\s+show router isis all (adjacency|interface)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSTANCE_HEADER = Pattern.compile("^Rtr Base ISIS Instance (\\d+) (Adjacency|Interfaces)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADJACENCY_ROW = Pattern.compile(
            "^(\\S+)\\s+(L1L2|L1|L2)\\s+(Up|Down)\\s+(\\d+)\\s+(.+?)\\s+(\\d+)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INTERFACE_ROW = Pattern.compile(
            "^(\\S+)\\s+(L1L2|L1|L2)\\s+(\\d+)\\s+(Up|Down)\\s+(\\S+)\\s+(\\S+)\\s*$",
            Pattern.CASE_INSENSITIVE);

    public ParsedRouter parse(String cliOutput) {
        List<IsisAdjacency> adjacencies = new ArrayList<>();
        List<IsisInterface> interfaces = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String hostname = null;
        int instance = -1;
        Section section = Section.NONE;

        for (String originalLine : cliOutput.replace("\r\n", "\n").split("\n")) {
            String line = originalLine.stripTrailing();
            Matcher prompt = PROMPT.matcher(line);
            if (prompt.matches()) {
                hostname = prompt.group(1).trim();
                continue;
            }

            Matcher header = INSTANCE_HEADER.matcher(line.trim());
            if (header.matches()) {
                instance = Integer.parseInt(header.group(1));
                section = "Adjacency".equalsIgnoreCase(header.group(2)) ? Section.ADJACENCY : Section.INTERFACE;
                continue;
            }

            if (isTableNoise(line) || section == Section.NONE) {
                continue;
            }

            if (section == Section.ADJACENCY) {
                parseAdjacencyLine(line, instance, adjacencies);
            } else {
                parseInterfaceLine(line, instance, interfaces);
            }
        }

        if (hostname == null) {
            warnings.add("Adjacency komut satırından yerel hostname okunamadı.");
        }
        for (IsisAdjacency adjacency : adjacencies) {
            if (adjacency.isUp() && interfaces.stream().noneMatch(item ->
                    item.instance() == adjacency.instance() && item.name().equals(adjacency.interfaceName()))) {
                warnings.add("Interface detayı bulunamadı: instance=" + adjacency.instance()
                        + ", interface=" + adjacency.interfaceName());
            }
        }
        return new ParsedRouter(hostname, adjacencies, interfaces, warnings);
    }

    private void parseAdjacencyLine(String line, int instance, List<IsisAdjacency> adjacencies) {
        Matcher row = ADJACENCY_ROW.matcher(line.trim());
        if (row.matches()) {
            adjacencies.add(new IsisAdjacency(
                    instance, row.group(1), row.group(2).toUpperCase(), row.group(3),
                    Integer.parseInt(row.group(4)), row.group(5).trim(), Integer.parseInt(row.group(6))));
            return;
        }

        // SR OS terminal genişliği dar olduğunda sadece Interface sütunu alt satıra taşar.
        if (!adjacencies.isEmpty() && startsWithIndent(line) && !line.trim().contains(" ")) {
            int last = adjacencies.size() - 1;
            IsisAdjacency previous = adjacencies.get(last);
            adjacencies.set(last, new IsisAdjacency(
                    previous.instance(), previous.neighborId(), previous.usage(), previous.state(),
                    previous.holdTimeSeconds(), previous.interfaceName() + line.trim(), previous.mtId()));
        }
    }

    private void parseInterfaceLine(String line, int instance, List<IsisInterface> interfaces) {
        Matcher row = INTERFACE_ROW.matcher(line.trim());
        if (!row.matches()) {
            return;
        }
        Integer[] metrics = parseMetrics(row.group(5));
        interfaces.add(new IsisInterface(
                instance, row.group(1), row.group(2).toUpperCase(), Integer.parseInt(row.group(3)),
                row.group(4), metrics[0], metrics[1], row.group(6)));
    }

    private Integer[] parseMetrics(String rawMetrics) {
        String[] parts = rawMetrics.split("/", -1);
        if (parts.length != 2) {
            return new Integer[] {null, null};
        }
        return new Integer[] {parseMetric(parts[0]), parseMetric(parts[1])};
    }

    private Integer parseMetric(String value) {
        return "-".equals(value) ? null : Integer.valueOf(value);
    }

    private boolean isTableNoise(String line) {
        String trimmed = line.trim();
        return trimmed.isEmpty()
                || trimmed.startsWith("===")
                || trimmed.startsWith("---")
                || trimmed.startsWith("System ID")
                || trimmed.startsWith("Interface")
                || trimmed.startsWith("Adjacencies")
                || trimmed.startsWith("Interfaces")
                || trimmed.startsWith("No Matching Entries")
                || trimmed.equals("State");
    }

    private boolean startsWithIndent(String line) {
        return !line.isEmpty() && Character.isWhitespace(line.charAt(0));
    }

    private enum Section {
        NONE, ADJACENCY, INTERFACE
    }
}
