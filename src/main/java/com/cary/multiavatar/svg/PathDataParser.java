package com.cary.multiavatar.svg;

import java.awt.geom.Path2D;

/**
 * SVG path 数据解析器：把 {@code d} 属性解析为 {@link Path2D}。
 *
 * <p>支持指令：M/m、L/l、H/h、V/v、C/c、S/s、Q/q、T/t、A/a、Z/z；
 * 支持隐式指令重复（如 {@code M a b c d} 中后一对按 L 处理）、逗号/空白分隔、
 * 符号与小数点前缀以及科学计数法（如 {@code 4e-5}）。
 * 圆弧（A/a）按 W3C SVG 1.1 附录 F.6 算法转换为三次贝塞尔曲线。</p>
 */
public final class PathDataParser {

    private PathDataParser() {
    }

    /**
     * 解析 path 数据。
     *
     * @param d SVG path 的 d 属性值（null/空 返回空 Path2D）
     */
    public static Path2D parse(String d) {
        Path2D path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        if (d == null || d.isEmpty()) {
            return path;
        }
        new Parser(d, path).run();
        return path;
    }

    /**
     * 解析状态机。
     */
    private static final class Parser {
        private final String s;
        private final Path2D path;
        private int i;

        private double cx;          // 当前点
        private double cy;
        private double sx;          // 子路径起点
        private double sy;
        private double prevCubicX;  // 上一个 C 指令第二控制点（供 S 反射）
        private double prevCubicY;
        private double prevQuadX;   // 上一个 Q 指令控制点（供 T 反射）
        private double prevQuadY;
        private char lastCmd;       // 上一个指令字母（用于隐式重复与反射）

        Parser(String s, Path2D path) {
            this.s = s;
            this.path = path;
        }

        /**
         * F.6.5 的 angle 函数（atan2 归一化到 [0, 2π)）。
         */
        private static double angle(double ux, double uy, double vx, double vy) {
            double dot = ux * vx + uy * vy;
            double len = Math.sqrt(ux * ux + uy * uy) * Math.sqrt(vx * vx + vy * vy);
            double a = Math.acos(Math.max(-1, Math.min(1, dot / len)));
            return (ux * vy - uy * vx < 0) ? -a : a;
        }

        private static boolean isLetter(char c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
        }

        void run() {
            while (i < s.length()) {
                skipSep();
                if (i >= s.length()) {
                    break;
                }
                char c = s.charAt(i);
                char cmd;
                if (isLetter(c)) {
                    cmd = c;
                    i++;
                } else if (lastCmd == 0) {
                    break; // 没有起始指令，忽略
                } else if (lastCmd == 'M') {
                    cmd = 'L';
                } else if (lastCmd == 'm') {
                    cmd = 'l';
                } else {
                    cmd = lastCmd; // 隐式重复上一指令
                }
                exec(cmd);
                lastCmd = cmd;
            }
        }

        private void exec(char cmd) {
            switch (cmd) {
                case 'M':
                    moveTo(readNum(), readNum(), false);
                    break;
                case 'm':
                    moveTo(readNum(), readNum(), true);
                    break;
                case 'L':
                    lineTo(readNum(), readNum(), false);
                    break;
                case 'l':
                    lineTo(readNum(), readNum(), true);
                    break;
                case 'H':
                    hLine(readNum(), false);
                    break;
                case 'h':
                    hLine(readNum(), true);
                    break;
                case 'V':
                    vLine(readNum(), false);
                    break;
                case 'v':
                    vLine(readNum(), true);
                    break;
                case 'C':
                    cubic(readNum(), readNum(), readNum(), readNum(), readNum(), readNum(), false);
                    break;
                case 'c':
                    cubic(readNum(), readNum(), readNum(), readNum(), readNum(), readNum(), true);
                    break;
                case 'S':
                    smoothCubic(readNum(), readNum(), readNum(), readNum(), false);
                    break;
                case 's':
                    smoothCubic(readNum(), readNum(), readNum(), readNum(), true);
                    break;
                case 'Q':
                    quad(readNum(), readNum(), readNum(), readNum(), false);
                    break;
                case 'q':
                    quad(readNum(), readNum(), readNum(), readNum(), true);
                    break;
                case 'T':
                    smoothQuad(readNum(), readNum(), false);
                    break;
                case 't':
                    smoothQuad(readNum(), readNum(), true);
                    break;
                case 'A':
                    arc(readNum(), readNum(), readNum(), readFlag(), readFlag(), readNum(), readNum(), false);
                    break;
                case 'a':
                    arc(readNum(), readNum(), readNum(), readFlag(), readFlag(), readNum(), readNum(), true);
                    break;
                case 'Z':
                case 'z':
                    path.closePath();
                    cx = sx;
                    cy = sy;
                    break;
                default:
                    break;
            }
        }

        private void moveTo(double x, double y, boolean rel) {
            if (rel) {
                x += cx;
                y += cy;
            }
            path.moveTo(x, y);
            cx = x;
            cy = y;
            sx = x;
            sy = y;
        }

        private void lineTo(double x, double y, boolean rel) {
            if (rel) {
                x += cx;
                y += cy;
            }
            path.lineTo(x, y);
            cx = x;
            cy = y;
        }

        private void hLine(double x, boolean rel) {
            cx = rel ? cx + x : x;
            path.lineTo(cx, cy);
        }

        private void vLine(double y, boolean rel) {
            cy = rel ? cy + y : y;
            path.lineTo(cx, cy);
        }

        private void cubic(double x1, double y1, double x2, double y2, double x, double y, boolean rel) {
            if (rel) {
                x1 += cx;
                y1 += cy;
                x2 += cx;
                y2 += cy;
                x += cx;
                y += cy;
            }
            path.curveTo(x1, y1, x2, y2, x, y);
            prevCubicX = x2;
            prevCubicY = y2;
            cx = x;
            cy = y;
        }

        private void smoothCubic(double x2, double y2, double x, double y, boolean rel) {
            // 反射控制点恒为绝对坐标（相对上一 C/S 的第二控制点关于当前点镜像）
            double x1 = cx, y1 = cy;
            if (lastCmd == 'C' || lastCmd == 'c' || lastCmd == 'S' || lastCmd == 's') {
                x1 = 2 * cx - prevCubicX;
                y1 = 2 * cy - prevCubicY;
            }
            // 终点与第二控制点按相对/绝对处理；不再走 cubic()（避免反射点被二次偏移）
            double ax2 = rel ? cx + x2 : x2;
            double ay2 = rel ? cy + y2 : y2;
            double ax = rel ? cx + x : x;
            double ay = rel ? cy + y : y;
            path.curveTo(x1, y1, ax2, ay2, ax, ay);
            prevCubicX = ax2;
            prevCubicY = ay2;
            cx = ax;
            cy = ay;
        }

        private void quad(double x1, double y1, double x, double y, boolean rel) {
            if (rel) {
                x1 += cx;
                y1 += cy;
                x += cx;
                y += cy;
            }
            // Path2D 无 quadTo，用三次贝塞尔等价表示：cp1 = (1/3)起 + (2/3)控, cp2 = (2/3)控 + (1/3)终
            double cx1 = cx + 2.0 / 3.0 * (x1 - cx);
            double cy1 = cy + 2.0 / 3.0 * (y1 - cy);
            double cx2 = x + 2.0 / 3.0 * (x1 - x);
            double cy2 = y + 2.0 / 3.0 * (y1 - y);
            path.curveTo(cx1, cy1, cx2, cy2, x, y);
            prevQuadX = x1;
            prevQuadY = y1;
            cx = x;
            cy = y;
        }

        private void smoothQuad(double x, double y, boolean rel) {
            // 反射控制点恒为绝对坐标
            double x1 = cx, y1 = cy;
            if (lastCmd == 'Q' || lastCmd == 'q' || lastCmd == 'T' || lastCmd == 't') {
                x1 = 2 * cx - prevQuadX;
                y1 = 2 * cy - prevQuadY;
            }
            double ax = rel ? cx + x : x;
            double ay = rel ? cy + y : y;
            // Path2D 无 quadTo，用三次贝塞尔等价表示
            double c1x = cx + 2.0 / 3.0 * (x1 - cx);
            double c1y = cy + 2.0 / 3.0 * (y1 - cy);
            double c2x = ax + 2.0 / 3.0 * (x1 - ax);
            double c2y = ay + 2.0 / 3.0 * (y1 - ay);
            path.curveTo(c1x, c1y, c2x, c2y, ax, ay);
            prevQuadX = x1;
            prevQuadY = y1;
            cx = ax;
            cy = ay;
        }

        // ---------- 数字读取 ----------

        /**
         * SVG 圆弧 → 三次贝塞尔（W3C SVG 1.1 F.6 标准实现）。
         */
        private void arc(double rx, double ry, double phiDeg, boolean largeArc, boolean sweep,
                         double x, double y, boolean rel) {
            double x2 = rel ? cx + x : x;
            double y2 = rel ? cy + y : y;
            double x1 = cx, y1 = cy;

            if (x1 == x2 && y1 == y2) {
                return; // 起点=终点，按规范忽略
            }
            rx = Math.abs(rx);
            ry = Math.abs(ry);
            if (rx == 0 || ry == 0) {
                path.lineTo(x2, y2); // 半径为零退化为直线
                cx = x2;
                cy = y2;
                return;
            }

            double phi = Math.toRadians(phiDeg % 360);
            double cosPhi = Math.cos(phi);
            double sinPhi = Math.sin(phi);

            // F.6.5 步骤 1：变换到椭圆中心坐标系
            double dx2 = (x1 - x2) / 2;
            double dy2 = (y1 - y2) / 2;
            double x1p = cosPhi * dx2 + sinPhi * dy2;
            double y1p = -sinPhi * dx2 + cosPhi * dy2;

            // F.6.6 步骤 2：半径校正
            double lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry);
            if (lambda > 1) {
                double sq = Math.sqrt(lambda);
                rx *= sq;
                ry *= sq;
            }

            // F.6.5 步骤 3：计算中心坐标（单位圆中）
            double rx2 = rx * rx, ry2 = ry * ry;
            double num = rx2 * ry2 - rx2 * y1p * y1p - ry2 * x1p * x1p;
            double den = rx2 * y1p * y1p + ry2 * x1p * x1p;
            double coef = (largeArc != sweep ? 1 : -1)
                    * Math.sqrt(Math.max(0, num / den));
            double cxp = coef * (rx * y1p / ry);
            double cyp = coef * (-ry * x1p / rx);

            // F.6.5 步骤 4：变换回原坐标系（注意：勿与成员 cx/cy 重名，以免遮蔽）
            double centerX = cosPhi * cxp - sinPhi * cyp + (x1 + x2) / 2;
            double centerY = sinPhi * cxp + cosPhi * cyp + (y1 + y2) / 2;

            // F.6.5 步骤 5：起始角与角度增量
            double theta1 = angle(1, 0, (x1p - cxp) / rx, (y1p - cyp) / ry);
            double dTheta = angle((x1p - cxp) / rx, (y1p - cyp) / ry,
                    (-x1p - cxp) / rx, (-y1p - cyp) / ry);
            if (!sweep && dTheta > 0) {
                dTheta -= 2 * Math.PI;
            } else if (sweep && dTheta < 0) {
                dTheta += 2 * Math.PI;
            }

            // 分段（每段不超过 90 度）
            int segments = (int) Math.ceil(Math.abs(dTheta) / (Math.PI / 2));
            segments = Math.max(1, segments);
            double seg = dTheta / segments;
            double theta = theta1;

            // 弧线从当前点(x1,y1)连续绘制，不新开子路径
            for (int k = 0; k < segments; k++) {
                double t2 = (k == segments - 1) ? theta1 + dTheta : theta + seg;
                double cosT = Math.cos(theta), sinT = Math.sin(theta);
                double cosT2 = Math.cos(t2), sinT2 = Math.sin(t2);

                double alpha = Math.sin(seg)
                        * (Math.sqrt(4 + 3 * Math.pow(Math.tan(seg / 2), 2)) - 1) / 3;

                double p1x = centerX + rx * cosT;
                double p1y = centerY + ry * sinT;
                double p2x = centerX + rx * cosT2;
                double p2y = centerY + ry * sinT2;

                double cp1x = p1x - alpha * rx * sinT;
                double cp1y = p1y + alpha * ry * cosT;
                double cp2x = p2x + alpha * rx * sinT2;
                double cp2y = p2y - alpha * ry * cosT2;

                path.curveTo(cp1x, cp1y, cp2x, cp2y, p2x, p2y);
                theta = t2;
            }
            cx = x2;
            cy = y2;
        }

        private double readNum() {
            skipSep();
            StringBuilder sb = new StringBuilder();
            if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+')) {
                sb.append(s.charAt(i++));
            }
            boolean dot = false;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (Character.isDigit(c)) {
                    sb.append(c);
                    i++;
                } else if (c == '.' && !dot) {
                    dot = true;
                    sb.append(c);
                    i++;
                } else if ((c == 'e' || c == 'E') && i + 1 < s.length()) {
                    // 科学计数法
                    char n = s.charAt(i + 1);
                    if (Character.isDigit(n) || n == '-' || n == '+') {
                        sb.append(c);
                        i++;
                        if (n == '-' || n == '+') {
                            sb.append(n);
                            i++;
                        }
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            if (sb.length() == 0) {
                return 0;
            }
            try {
                return Double.parseDouble(sb.toString());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        /**
         * 读取 0/1 标志。
         */
        private boolean readFlag() {
            skipSep();
            if (i < s.length() && (s.charAt(i) == '0' || s.charAt(i) == '1')) {
                return s.charAt(i++) == '1';
            }
            return false;
        }

        private void skipSep() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == ',') {
                    i++;
                } else {
                    break;
                }
            }
        }
    }
}
