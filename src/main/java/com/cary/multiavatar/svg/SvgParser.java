package com.cary.multiavatar.svg;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.geom.Path2D;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * SVG XML 解析器：把 SVG 字符串解析为 {@link SvgDocument} 领域模型。
 *
 * <p>使用 JDK 内置的 DOM 解析器（javax.xml.parsers），并做 XXE 防护：
 * 禁用 DOCTYPE 声明与外部实体，仅解析本地 XML 数据。</p>
 */
public final class SvgParser {

    /**
     * viewBox 缺失时的兜底视口。
     */
    private static final double[] FALLBACK_VIEW = {0, 0, 256, 256};

    private SvgParser() {
    }

    /**
     * 解析 SVG 字符串。
     *
     * @param svg 完整 SVG（含根元素）；null/空返回空文档
     */
    public static SvgDocument parse(String svg) {
        if (svg == null || svg.trim().isEmpty()) {
            return new SvgDocument(0, 0, 0, 0, new ArrayList<SvgShape>());
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // XXE 防护：禁用外部实体与 DTD
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(svg)));

            Element root = doc.getDocumentElement();
            double[] view = parseViewBox(root.getAttribute("viewBox"));

            List<SvgShape> shapes = new ArrayList<>();
            collect(root, shapes);
            return new SvgDocument(view[0], view[1], view[2], view[3], shapes);
        } catch (Exception e) {
            throw new IllegalArgumentException("SVG 解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 递归收集形状元素（保持文档顺序）。
     */
    private static void collect(Node node, List<SvgShape> out) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element el = (Element) child;
            String tag = el.getTagName();
            if ("path".equals(tag)) {
                SvgPath p = parsePath(el);
                if (p != null) {
                    out.add(p);
                }
            } else if ("polygon".equals(tag)) {
                SvgPolygon p = parsePolygon(el);
                if (p != null) {
                    out.add(p);
                }
            } else if ("line".equals(tag)) {
                out.add(parseLine(el));
            } else if ("rect".equals(tag)) {
                out.add(parseRect(el));
            }
            collect(child, out);
        }
    }

    private static SvgPath parsePath(Element el) {
        Path2D path = PathDataParser.parse(el.getAttribute("d"));
        if (path.getCurrentPoint() == null) {
            return null; // 无有效几何
        }
        return new SvgPath(path, SvgStyle.parse(el.getAttribute("style")),
                TransformParser.parse(el.getAttribute("transform")));
    }

    private static SvgPolygon parsePolygon(Element el) {
        Path2D path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        double[] pts = parsePoints(el.getAttribute("points"));
        if (pts.length < 2) {
            return null;
        }
        path.moveTo(pts[0], pts[1]);
        for (int i = 2; i + 1 < pts.length; i += 2) {
            path.lineTo(pts[i], pts[i + 1]);
        }
        path.closePath();
        return new SvgPolygon(path, SvgStyle.parse(el.getAttribute("style")),
                TransformParser.parse(el.getAttribute("transform")));
    }

    private static SvgLine parseLine(Element el) {
        return new SvgLine(
                num(el.getAttribute("x1")), num(el.getAttribute("y1")),
                num(el.getAttribute("x2")), num(el.getAttribute("y2")),
                SvgStyle.parse(el.getAttribute("style")),
                TransformParser.parse(el.getAttribute("transform")));
    }

    private static SvgRect parseRect(Element el) {
        double x = num(el.getAttribute("x"));
        double y = num(el.getAttribute("y"));
        double w = num(el.getAttribute("width"));
        double h = num(el.getAttribute("height"));
        double rx = attr(el, "rx", -1);
        double ry = attr(el, "ry", -1);
        if (rx < 0) {
            rx = ry < 0 ? 0 : ry;
        }
        if (ry < 0) {
            ry = rx;
        }
        return new SvgRect(x, y, w, h, rx, ry,
                SvgStyle.parse(el.getAttribute("style")),
                TransformParser.parse(el.getAttribute("transform")));
    }

    /**
     * 解析 viewBox（"minX minY width height" 或 "width height"）。
     */
    private static double[] parseViewBox(String vb) {
        if (vb == null || vb.trim().isEmpty()) {
            return FALLBACK_VIEW.clone();
        }
        String[] parts = vb.trim().split("[,\\s]+");
        double[] out = new double[4];
        try {
            if (parts.length >= 4) {
                for (int i = 0; i < 4; i++) {
                    out[i] = Double.parseDouble(parts[i]);
                }
            } else if (parts.length == 2) {
                out[2] = Double.parseDouble(parts[0]);
                out[3] = Double.parseDouble(parts[1]);
            } else {
                return FALLBACK_VIEW.clone();
            }
        } catch (NumberFormatException e) {
            return FALLBACK_VIEW.clone();
        }
        return out;
    }

    /**
     * 解析 points 点列（逗号/空白分隔的坐标对）。
     */
    private static double[] parsePoints(String points) {
        if (points == null || points.trim().isEmpty()) {
            return new double[0];
        }
        String[] parts = points.trim().split("[,\\s]+");
        double[] out = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = num(parts[i]);
        }
        return out;
    }

    private static double attr(Element el, String name, double dft) {
        String v = el.getAttribute(name);
        return (v == null || v.isEmpty()) ? dft : num(v);
    }

    private static double num(String v) {
        if (v == null || v.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
