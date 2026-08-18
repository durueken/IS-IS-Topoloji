package com.nokia.topology.export;

import com.nokia.topology.domain.Device;
import com.nokia.topology.domain.DeviceTier;
import com.nokia.topology.domain.Link;
import com.nokia.topology.domain.LinkCapacity;
import com.nokia.topology.domain.Topology;
import com.nokia.topology.layout.HierarchicalLayout;
import com.nokia.topology.layout.NodePosition;

import java.util.HashMap;
import java.util.Map;

/** Topolojiyi Draw.io'nun açabildiği düzenlenebilir XML biçimine dönüştürür. */
public final class DrawioExporter {
    // Önceki değerler: -0.78 ve 0.78. Geri dönmek için yalnız bu iki değeri değiştirmen yeterlidir.
    private static final double SOURCE_INTERFACE_LABEL_POSITION = -0.35;
    private static final double TARGET_INTERFACE_LABEL_POSITION = 0.35;

    public String export(Topology topology) {
        StringBuilder xml = new StringBuilder("<mxfile host=\"app.diagrams.net\"><diagram name=\"Topology\"><mxGraphModel><root>");
        xml.append("<mxCell id=\"0\"/><mxCell id=\"1\" parent=\"0\"/>");

        Map<String, String> ids = new HashMap<>();
        HierarchicalLayout layout = new HierarchicalLayout();
        Map<String, NodePosition> positions = layout.layout(topology);
        int index = 2;
        for (Device device : topology.devices()) {
            String id = "device-" + index++;
            ids.put(device.systemId(), id);
            NodeSize size = nodeSize(device);
            xml.append("<mxCell id=\"").append(id).append("\" value=\"")
                    .append(deviceLabel(device))
                    .append("\" style=\"").append(nodeStyle(device.tier())).append("\" vertex=\"1\" parent=\"1\">");
            NodePosition position = positions.get(device.systemId());
            double x = position.x() - size.width() / 2;
            double y = position.y() - size.height() / 2;
            xml.append("<mxGeometry x=\"").append(x)
                    .append("\" y=\"").append(y)
                    .append("\" width=\"").append(size.width()).append("\" height=\"").append(size.height())
                    .append("\" as=\"geometry\"/></mxCell>");
        }
        for (Link link : topology.links()) {
            String edgeId = "link-" + index++;
            xml.append("<mxCell id=\"").append(edgeId).append("\" value=\"\"")
                    .append(" style=\"").append(linkStyle()).append("\" edge=\"1\" parent=\"1\" source=\"").append(ids.get(link.source().systemId()))
                    .append("\" target=\"").append(ids.get(link.target().systemId()))
                    .append("\"><mxGeometry relative=\"1\" as=\"geometry\"/></mxCell>");
            // Uçlardaki etiketler her cihazın kendi IS-IS interface bilgisini gösterir.
            if (link.sourceInterface() != null && !link.sourceInterface().isBlank()) {
                appendInterfaceLabel(xml, "label-" + index++, edgeId, link.sourceInterface(),
                        SOURCE_INTERFACE_LABEL_POSITION);
            }
            appendEdgeLabel(xml, "label-" + index++, edgeId, mainTopologyLinkLabel(link), 0);
            if (link.targetInterface() != null && !link.targetInterface().isBlank()) {
                appendInterfaceLabel(xml, "label-" + index++, edgeId, link.targetInterface(),
                        TARGET_INTERFACE_LABEL_POSITION);
            }
        }
        xml.append("</root></mxGraphModel></diagram>");
        return xml.append("</mxfile>").toString();
    }

    private String nodeStyle(DeviceTier tier) {
        return switch (tier) {
            case T2_CORE -> "shape=hexagon;perimeter=hexagonPerimeter;whiteSpace=wrap;html=1;fillColor=#DDEBF7;";
            case T3_AGGREGATION -> "rounded=1;whiteSpace=wrap;html=1;fillColor=#E2F0D9;";
            case T4_ACCESS -> "shape=ellipse;whiteSpace=wrap;html=1;fillColor=#FCE4D6;";
            case UNKNOWN -> "shape=rectangle;whiteSpace=wrap;html=1;fillColor=#F2F2F2;";
        };
    }

    private String deviceLabel(Device device) {
        if (device.hostname().equals(device.systemId())) {
            return escape(device.hostname());
        }
        return escape(device.hostname()) + "&lt;br&gt;" + escape(device.systemId());
    }

    private NodeSize nodeSize(Device device) {
        int longestText = Math.max(device.hostname().length(), device.systemId().length());
        double width = Math.max(140, 42 + longestText * 7.5);
        double height = device.hostname().equals(device.systemId()) ? 52 : 72;
        return new NodeSize(width, height);
    }

    private String linkStyle() {
        return "edgeStyle=none;rounded=1;endArrow=none;strokeColor=#666666;";
    }

    private void appendEdgeLabel(StringBuilder xml, String id, String edgeId, String value, double relativeX) {
        xml.append("<mxCell id=\"").append(id).append("\" value=\"").append(escape(value))
                .append("\" style=\"edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;fontSize=9;\"")
                .append(" vertex=\"1\" connectable=\"0\" parent=\"").append(edgeId).append("\">")
                .append("<mxGeometry x=\"").append(relativeX)
                .append("\" relative=\"1\" as=\"geometry\"><mxPoint x=\"0\" y=\"-14\" as=\"offset\"/>")
                .append("</mxGeometry></mxCell>");
    }

    /** Uzun interface isimleri için merkez etiketinden daha küçük bir yazı kullanır. */
    private void appendInterfaceLabel(StringBuilder xml, String id, String edgeId, String value, double relativeX) {
        xml.append("<mxCell id=\"").append(id).append("\" value=\"").append(escape(value))
                .append("\" style=\"edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;fontSize=7;\"")
                .append(" vertex=\"1\" connectable=\"0\" parent=\"").append(edgeId).append("\">")
                .append("<mxGeometry x=\"").append(relativeX)
                .append("\" relative=\"1\" as=\"geometry\"><mxPoint x=\"0\" y=\"-10\" as=\"offset\"/>")
                .append("</mxGeometry></mxCell>");
    }

    /** Ana topolojide bağlantının orta noktasında görünen IS-IS özeti. */
    private String mainTopologyLinkLabel(Link link) {
        return "IS-IS(I:" + valueOrDash(link.isisInstance()) + "." + valueOrDash(link.level())
                + ".M:" + valueOrDash(link.metric()) + ")";
    }

    private String valueOrDash(Object value) {
        return value == null ? "-" : value.toString();
    }

    private record NodeSize(double width, double height) {
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
