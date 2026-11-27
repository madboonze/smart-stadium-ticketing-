package com.smartstadium.ticketing;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Small self-contained QR code generator (byte mode, error correction level M, versions 1-10).
 * No external library is needed, so pom.xml stays unchanged.
 * Up to 213 bytes of text fit, which is far more than a ticket code needs.
 */
public final class QrCode {
    public static final int MAX_BYTES = 213;

    // Error correction level M, versions 1..10 (index 0 unused).
    private static final int[] EC_PER_BLOCK = {0, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26};
    private static final int[] G1_BLOCKS    = {0, 1, 1, 1, 2, 2, 4, 4, 2, 3, 4};
    private static final int[] G1_DATA      = {0, 16, 28, 44, 32, 43, 27, 31, 38, 36, 43};
    private static final int[] G2_BLOCKS    = {0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 1};
    private static final int[] REMAINDER    = {0, 0, 7, 7, 7, 7, 7, 0, 0, 0, 0};
    private static final int[][] ALIGNMENT = {
            {}, {}, {6, 18}, {6, 22}, {6, 26}, {6, 30}, {6, 34},
            {6, 22, 38}, {6, 24, 42}, {6, 26, 46}, {6, 28, 50}
    };

    private final int version;
    private final int size;
    private final boolean[][] modules;     // [y][x], true = dark
    private final boolean[][] isFunction;  // finder, timing, alignment, format and version areas

    private QrCode(int version) {
        this.version = version;
        this.size = version * 4 + 17;
        this.modules = new boolean[size][size];
        this.isFunction = new boolean[size][size];
    }

    /** Encodes text (UTF-8) into the smallest QR code that fits. */
    public static QrCode encode(String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("QR text must not be empty.");
        }
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        for (int v = 1; v <= 10; v++) {
            int countBits = v <= 9 ? 8 : 16;
            int capacityBits = dataCodewords(v) * 8;
            if (4 + countBits + data.length * 8 <= capacityBits) {
                QrCode qr = new QrCode(v);
                qr.build(data, countBits, capacityBits);
                return qr;
            }
        }
        throw new IllegalArgumentException("Text is too long for a QR code (max " + MAX_BYTES + " bytes).");
    }

    public int getVersion() { return version; }
    public int getSize() { return size; }

    /** True if the module at column x, row y is dark. */
    public boolean isDark(int x, int y) {
        return x >= 0 && x < size && y >= 0 && y < size && modules[y][x];
    }

    /** Returns the QR code as an SVG image, with a quiet zone of the given width in modules. */
    public String toSvg(int border) {
        if (border < 0) throw new IllegalArgumentException("Border must not be negative.");
        int dim = size + border * 2;
        StringBuilder path = new StringBuilder();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (modules[y][x]) {
                    path.append('M').append(x + border).append(',').append(y + border).append("h1v1h-1z");
                }
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + dim + " " + dim
                + "\" shape-rendering=\"crispEdges\">"
                + "<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>"
                + "<path d=\"" + path + "\" fill=\"#000000\"/></svg>";
    }

    // ---------------------------------------------------------------- building

    private void build(byte[] data, int countBits, int capacityBits) {
        drawFunctionPatterns();
        byte[] codewords = addErrorCorrection(encodeData(data, countBits, capacityBits));
        drawCodewords(codewords);

        int bestMask = 0;
        int bestPenalty = Integer.MAX_VALUE;
        for (int mask = 0; mask < 8; mask++) {
            applyMask(mask);
            drawFormatBits(mask);
            int penalty = penaltyScore();
            if (penalty < bestPenalty) {
                bestPenalty = penalty;
                bestMask = mask;
            }
            applyMask(mask); // XOR again to undo
        }
        applyMask(bestMask);
        drawFormatBits(bestMask);
    }

    private static int totalCodewords(int v) {
        int g1 = G1_BLOCKS[v] * (G1_DATA[v] + EC_PER_BLOCK[v]);
        int g2 = G2_BLOCKS[v] * (G1_DATA[v] + 1 + EC_PER_BLOCK[v]);
        return g1 + g2;
    }

    private static int dataCodewords(int v) {
        return G1_BLOCKS[v] * G1_DATA[v] + G2_BLOCKS[v] * (G1_DATA[v] + 1);
    }

    private static byte[] encodeData(byte[] data, int countBits, int capacityBits) {
        BitBuffer bb = new BitBuffer();
        bb.append(0b0100, 4);                 // byte mode
        bb.append(data.length, countBits);
        for (byte b : data) bb.append(b & 0xFF, 8);
        bb.append(0, Math.min(4, capacityBits - bb.length)); // terminator
        bb.append(0, (8 - bb.length % 8) % 8);               // pad to a whole byte
        for (int pad = 0xEC; bb.length < capacityBits; pad ^= 0xEC ^ 0x11) {
            bb.append(pad, 8);
        }
        return bb.toBytes();
    }

    private byte[] addErrorCorrection(byte[] data) {
        int ecLen = EC_PER_BLOCK[version];
        int numBlocks = G1_BLOCKS[version] + G2_BLOCKS[version];
        byte[] divisor = reedSolomonDivisor(ecLen);

        byte[][] dataBlocks = new byte[numBlocks][];
        byte[][] ecBlocks = new byte[numBlocks][];
        int pos = 0;
        for (int i = 0; i < numBlocks; i++) {
            int len = G1_DATA[version] + (i < G1_BLOCKS[version] ? 0 : 1);
            dataBlocks[i] = Arrays.copyOfRange(data, pos, pos + len);
            ecBlocks[i] = reedSolomonRemainder(dataBlocks[i], divisor);
            pos += len;
        }

        byte[] result = new byte[totalCodewords(version)];
        int k = 0;
        for (int i = 0; i <= G1_DATA[version]; i++) {
            for (byte[] block : dataBlocks) {
                if (i < block.length) result[k++] = block[i];
            }
        }
        for (int i = 0; i < ecLen; i++) {
            for (byte[] block : ecBlocks) result[k++] = block[i];
        }
        return result;
    }

    private void drawFunctionPatterns() {
        for (int i = 0; i < size; i++) {
            setFunction(6, i, i % 2 == 0);
            setFunction(i, 6, i % 2 == 0);
        }
        drawFinder(3, 3);
        drawFinder(size - 4, 3);
        drawFinder(3, size - 4);

        int[] pos = ALIGNMENT[version];
        int last = pos.length - 1;
        for (int i = 0; i < pos.length; i++) {
            for (int j = 0; j < pos.length; j++) {
                boolean overlapsFinder = (i == 0 && j == 0) || (i == 0 && j == last) || (i == last && j == 0);
                if (!overlapsFinder) drawAlignment(pos[i], pos[j]);
            }
        }
        drawFormatBits(0); // reserve the area; real bits are drawn after masking
        drawVersionBits();
    }

    private void drawFinder(int cx, int cy) {
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                int x = cx + dx, y = cy + dy;
                if (x >= 0 && x < size && y >= 0 && y < size) {
                    int dist = Math.max(Math.abs(dx), Math.abs(dy));
                    setFunction(x, y, dist != 2 && dist != 4);
                }
            }
        }
    }

    private void drawAlignment(int cx, int cy) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                setFunction(cx + dx, cy + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
            }
        }
    }

    private void drawFormatBits(int mask) {
        int data = mask; // level M has format bits 00
        int rem = data;
        for (int i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
        int bits = ((data << 10) | rem) ^ 0x5412;

        for (int i = 0; i <= 5; i++) setFunction(8, i, bit(bits, i));
        setFunction(8, 7, bit(bits, 6));
        setFunction(8, 8, bit(bits, 7));
        setFunction(7, 8, bit(bits, 8));
        for (int i = 9; i < 15; i++) setFunction(14 - i, 8, bit(bits, i));

        for (int i = 0; i < 8; i++) setFunction(size - 1 - i, 8, bit(bits, i));
        for (int i = 8; i < 15; i++) setFunction(8, size - 15 + i, bit(bits, i));
        setFunction(8, size - 8, true); // always-dark module
    }

    private void drawVersionBits() {
        if (version < 7) return;
        int rem = version;
        for (int i = 0; i < 12; i++) rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
        int bits = (version << 12) | rem;
        for (int i = 0; i < 18; i++) {
            boolean dark = bit(bits, i);
            int a = size - 11 + i % 3;
            int b = i / 3;
            setFunction(a, b, dark);
            setFunction(b, a, dark);
        }
    }

    private void drawCodewords(byte[] data) {
        int totalBits = data.length * 8 + REMAINDER[version];
        int i = 0;
        for (int right = size - 1; right >= 1; right -= 2) {
            if (right == 6) right = 5; // skip the vertical timing column
            for (int vert = 0; vert < size; vert++) {
                for (int j = 0; j < 2; j++) {
                    int x = right - j;
                    boolean upward = ((right + 1) & 2) == 0;
                    int y = upward ? size - 1 - vert : vert;
                    if (!isFunction[y][x] && i < totalBits) {
                        if (i < data.length * 8) {
                            modules[y][x] = bit(data[i >>> 3] & 0xFF, 7 - (i & 7));
                        }
                        i++;
                    }
                }
            }
        }
    }

    private void applyMask(int mask) {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean invert = switch (mask) {
                    case 0 -> (x + y) % 2 == 0;
                    case 1 -> y % 2 == 0;
                    case 2 -> x % 3 == 0;
                    case 3 -> (x + y) % 3 == 0;
                    case 4 -> (x / 3 + y / 2) % 2 == 0;
                    case 5 -> x * y % 2 + x * y % 3 == 0;
                    case 6 -> (x * y % 2 + x * y % 3) % 2 == 0;
                    case 7 -> ((x + y) % 2 + x * y % 3) % 2 == 0;
                    default -> throw new IllegalArgumentException("Bad mask " + mask);
                };
                if (invert && !isFunction[y][x]) modules[y][x] = !modules[y][x];
            }
        }
    }

    /** Standard QR penalty rules, used to pick the mask that scans most reliably. */
    private int penaltyScore() {
        int penalty = 0;
        for (int a = 0; a < size; a++) {
            penalty += runPenalty(a, true) + runPenalty(a, false);
        }
        for (int y = 0; y < size - 1; y++) {
            for (int x = 0; x < size - 1; x++) {
                boolean c = modules[y][x];
                if (c == modules[y][x + 1] && c == modules[y + 1][x] && c == modules[y + 1][x + 1]) penalty += 3;
            }
        }
        boolean[] p1 = {true, false, true, true, true, false, true, false, false, false, false};
        boolean[] p2 = {false, false, false, false, true, false, true, true, true, false, true};
        for (int a = 0; a < size; a++) {
            for (int b = 0; b + 11 <= size; b++) {
                if (matches(a, b, p1, true) || matches(a, b, p2, true)) penalty += 40;
                if (matches(a, b, p1, false) || matches(a, b, p2, false)) penalty += 40;
            }
        }
        int dark = 0;
        for (boolean[] row : modules) for (boolean m : row) if (m) dark++;
        int total = size * size;
        int k = (Math.abs(dark * 20 - total * 10) + total - 1) / total - 1;
        penalty += Math.max(0, k) * 10;
        return penalty;
    }

    private int runPenalty(int line, boolean horizontal) {
        int penalty = 0, run = 0;
        boolean prev = false;
        for (int i = 0; i < size; i++) {
            boolean c = horizontal ? modules[line][i] : modules[i][line];
            if (i > 0 && c == prev) {
                run++;
            } else {
                if (run >= 5) penalty += run - 2;
                run = 1;
                prev = c;
            }
        }
        if (run >= 5) penalty += run - 2;
        return penalty;
    }

    private boolean matches(int line, int start, boolean[] pattern, boolean horizontal) {
        for (int i = 0; i < pattern.length; i++) {
            boolean c = horizontal ? modules[line][start + i] : modules[start + i][line];
            if (c != pattern[i]) return false;
        }
        return true;
    }

    private void setFunction(int x, int y, boolean dark) {
        modules[y][x] = dark;
        isFunction[y][x] = true;
    }

    private static boolean bit(int value, int i) {
        return ((value >>> i) & 1) != 0;
    }

    // ---------------------------------------------------------------- Reed-Solomon over GF(256)

    private static byte[] reedSolomonDivisor(int degree) {
        byte[] result = new byte[degree];
        result[degree - 1] = 1;
        int root = 1;
        for (int i = 0; i < degree; i++) {
            for (int j = 0; j < degree; j++) {
                result[j] = (byte) gfMultiply(result[j] & 0xFF, root);
                if (j + 1 < degree) result[j] ^= result[j + 1];
            }
            root = gfMultiply(root, 0x02);
        }
        return result;
    }

    private static byte[] reedSolomonRemainder(byte[] data, byte[] divisor) {
        byte[] result = new byte[divisor.length];
        for (byte b : data) {
            int factor = (b ^ result[0]) & 0xFF;
            System.arraycopy(result, 1, result, 0, result.length - 1);
            result[result.length - 1] = 0;
            for (int i = 0; i < result.length; i++) {
                result[i] ^= (byte) gfMultiply(divisor[i] & 0xFF, factor);
            }
        }
        return result;
    }

    private static int gfMultiply(int x, int y) {
        int z = 0;
        for (int i = 7; i >= 0; i--) {
            z = (z << 1) ^ ((z >>> 7) * 0x11D);
            z ^= ((y >>> i) & 1) * x;
        }
        return z;
    }

    private static final class BitBuffer {
        private byte[] bytes = new byte[64];
        private int length;

        void append(int value, int count) {
            for (int i = count - 1; i >= 0; i--) {
                if (length / 8 >= bytes.length) bytes = Arrays.copyOf(bytes, bytes.length * 2);
                if (((value >>> i) & 1) != 0) bytes[length / 8] |= (byte) (0x80 >>> (length % 8));
                length++;
            }
        }

        byte[] toBytes() {
            return Arrays.copyOf(bytes, length / 8);
        }
    }
}
