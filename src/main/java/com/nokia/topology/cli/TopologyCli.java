package com.nokia.topology.cli;

import com.nokia.topology.domain.Topology;
import com.nokia.topology.engine.TopologyBuilder;
import com.nokia.topology.export.DrawioExporter;
import com.nokia.topology.parser.NokiaIsisParser;
import com.nokia.topology.parser.ParsedRouter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** CLI dosyalarından Draw.io topolojisi üreten uygulama giriş noktası. */
public final class TopologyCli {
    private TopologyCli() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Kullanım: TopologyCli <cli-dosyası-veya-klasörü> <cikti.drawio>");
            System.exit(2);
        }
        Result result = generate(Path.of(args[0]), Path.of(args[1]));
        System.out.printf("Topoloji üretildi: %s (%d cihaz, %d bağlantı)%n",
                args[1], result.deviceCount(), result.linkCount());
    }

    public static Result generate(Path input, Path output) throws IOException {
        List<Path> files = inputFiles(input);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("Okunacak .txt veya .log dosyası bulunamadı: " + input);
        }
        NokiaIsisParser parser = new NokiaIsisParser();
        List<ParsedRouter> routers = files.stream().map(path -> readAndParse(path, parser)).toList();
        Topology topology = new TopologyBuilder().build(routers);
        Files.writeString(output, new DrawioExporter().export(topology), StandardCharsets.UTF_8);
        return new Result(topology.devices().size(), topology.links().size());
    }

    private static List<Path> inputFiles(Path input) throws IOException {
        if (Files.isRegularFile(input)) {
            return List.of(input);
        }
        try (Stream<Path> paths = Files.list(input)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".txt") || name.endsWith(".log");
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    private static ParsedRouter readAndParse(Path path, NokiaIsisParser parser) {
        try {
            return parser.parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalArgumentException("CLI dosyası okunamadı: " + path, exception);
        }
    }

    public record Result(
            int deviceCount,
            int linkCount) {
    }
}
