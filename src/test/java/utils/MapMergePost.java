package utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MapMergePost {

    public static void main(String[] args) throws Exception {
        File file = new File("./doc/merge.png");
        BufferedImage img = ImageIO.read(file);
        int w = img.getWidth();
        int h = img.getHeight();
        File out0 = new File("./doc/post_merge.png");
        if (!out0.exists())
            out0.createNewFile();
        int progress = 0;
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int size = blockSize(img, i, j, 16, 16);
                if (size < 16) {
                    int col = img.getRGB(i, j) & 0xFFFFFF;
                    int rep = find(collect(img, i, j, 5, 1, col), col);
                    img.setRGB(i, j, rep);
                }
            }
            if (100 * i / w > progress) {
                progress = 100 * i / w;
                System.out.println(progress + "%");
            }
        }
        ImageIO.write(img, "PNG", out0);
    }

    private static int distance(int a, int b) {
        int b0 = a & 0xFF;
        int b1 = b & 0xFF;
        int g0 = a >> 8 & 0xFF;
        int g1 = b >> 8 & 0xFF;
        int r0 = a >> 8 & 0xFF;
        int r1 = b >> 8 & 0xFF;
        return (b0 - b1) * (b0 - b1) + (g0 - g1) * (g0 - g1) + (r0 - r1) * (r0 - r1);
    }

    private static List<Integer> reduce(Map<Integer, Integer> map, int size) {
        return map.entrySet().stream()
                .sorted((a, b) -> -a.getValue().compareTo(b.getValue()))
                .map(Map.Entry::getKey).limit(size).collect(Collectors.toList());
    }

    private static int find(List<Integer> list, int col) {
        int min = Integer.MAX_VALUE;
        int val = 0xFF0000;
        for (int rep : list) {
            if (distance(col, rep) < min) {
                min = distance(col, rep);
                val = rep;
            }
        }
        return val;
    }

    private static List<Integer> collect(BufferedImage img, int x, int y, int r, int max, int not) {
        int w = img.getWidth();
        int h = img.getHeight();
        Map<Integer, Integer> ans = new HashMap<>();
        for (int i = x - r; i <= x + r; i++)
            for (int j = y - r; j <= y + r; j++) {
                if (i < 0 || j < 0 || i >= w || j >= h)
                    continue;
                int col = img.getRGB(i, j) & 0xFFFFFF;
                if (col == not)
                    continue;
                if (ans.containsKey(col))
                    ans.put(col, ans.get(col) + 1);
                else
                    ans.put(col, 1);

            }
        ans.remove(0x678284);
        ans.remove(0xD8B381);
        return reduce(ans, max);
    }

    private static final int[][] DIRE = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

    private static int blockSize(BufferedImage img, int x, int y, int r, int max) {
        int w = img.getWidth();
        int h = img.getHeight();
        int base = img.getRGB(x, y) & 0xFFFFFF;
        int count = 1;
        Queue<Point> queue = new ArrayDeque<>();
        Set<Point> pool = new HashSet<>();
        Point first = new Point(img, x, y);
        queue.add(first);
        pool.add(first);
        while (queue.size() > 0) {
            Point p = queue.poll();
            for (int[] dire : DIRE) {
                int nx = p.x + dire[0];
                int ny = p.y + dire[1];
                if (nx < 0 || nx >= w ||
                        ny < 0 || ny >= h ||
                        nx < x - r || nx > x + r ||
                        ny < y - r || ny > y + r)
                    continue;
                Point next = new Point(img, nx, ny);
                if (next.col != base || pool.contains(next))
                    continue;
                pool.add(next);
                queue.add(next);
                if (pool.size() > max)
                    return max;
            }
        }
        return pool.size();
    }

    private static class Point implements Comparable<Point> {
        int x, y, col;

        public Point(BufferedImage img, int x, int y) {
            this.x = x;
            this.y = y;
            this.col = img.getRGB(x, y) & 0xFFFFFF;
        }

        public int hashCode() {
            return x << 16 | y;
        }


        @Override
        public int compareTo(Point o) {
            return Integer.compare(hashCode(), o.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            return hashCode() == obj.hashCode();
        }
    }

}
