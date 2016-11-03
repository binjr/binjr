package eu.fthevenet.binjr.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by FTT2 on 02/11/2016.
 */
public class RamerDouglasPeucker {


//    public static List<Point> simplify(List<Point> points, double epsilon){
//        if (points.size() == 0)
//            return points;
//        return simplify(points, epsilon);
//    }

    public static List<Point> simplify(List<Point> points, double epsilon) {
        if (points.size() < 2) {
            return points;
        }
        List<Point> resultList =  new ArrayList<>();
        double dmax = -1.0;
        int index = 0;
        int end = points.size() -1;
        for (int i = 1; i < end-1; i++) {
            double d = perpendicularDistance(points.get(i), points.get(0), points.get(end));
            if (d > dmax) {
                dmax = d;
                index = i;
            }
        }
        if (dmax > epsilon) {
            List<Point> res1 = simplify(points.subList(0,index), epsilon);
            List<Point> res2 = simplify(points.subList(index, end+1), epsilon);
            resultList.addAll(res1);
            resultList.addAll(res2);
        }
        else {
            resultList.add(points.get(0));
            resultList.add(points.get(end));
        }
        return resultList;
    }

    private static double perpendicularDistance(Point p, Point a, Point b) {
        if (a.equals(b)){
            return p.distance(a);
        }
//        double r = ((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / ((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y));
//        if (r <= 0.0) return p.distance(a);
//        if (r >= 1.0) return p.distance(b);
//        double s = ((a.y - p.y) * (b.x - a.x) - (a.x - p.x) * (b.y - a.y)) / ((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y));
//        return Math.abs(s) * Math.sqrt(((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)));


        double n = Math.abs((b.x - a.x) * (a.y - p.y) - (a.x - p.x) * (b.y - a.y));
        double d = Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y));
        return n / d;
    }

    public static class Point {
        final public double y;
        final public double x;

        public Point(double x, double y) {
            this.y = y;
            this.x = x;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Point)) {
                return false;
            }
            Point p = (Point) obj;
            return x == p.x && y == p.y;
        }

        public double distance(Point p) {
            double dx = x - p.x;
            double dy = y - p.y;

            return Math.sqrt(dx * dx + dy * dy);
        }
    }
}
