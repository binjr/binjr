package eu.fthevenet.binjr.data;

import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Created by FTT2 on 02/11/2016.
 */
public class RamerDouglasPeucker {

private static final Logger logger = LogManager.getLogger(RamerDouglasPeucker.class);

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
            logger.info("d = " + d);
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


    /// <summary>
/// Ramer-Douglas-Peucker algorithm which reduces a series of points
/// to a simplified version that loses detail,
/// but maintains the general shape of the series.
/// </summary>
    private static BitSet getReductionMask(List<Point> points, double epsilon) {
        Stack<Pair<Integer, Integer>> stk = new Stack<>();
        int startIndex = 0;
        int lastIndex = points.size() - 1;
        stk.push(new Pair<>(startIndex, lastIndex));
        BitSet bitMask = new BitSet(lastIndex+1);
        bitMask.set(0, lastIndex, true);

        while (stk.size() > 0) {
            startIndex = stk.peek().getKey();
            lastIndex = stk.peek().getValue();
            stk.pop();

            double dmax = 0f;
            int index = startIndex;

            for (int i = index + 1; i < lastIndex; ++i) {
                if (bitMask.get(i)) {
                    double d = perpendicularDistance(points.get(i), points.get(startIndex), points.get(lastIndex));
                    if (d > dmax) {
                        index = i;
                        dmax = d;
                    }
                }
            }

            if (dmax > epsilon) {
                stk.push(new Pair<>(startIndex, index));
                stk.push(new Pair<>(index, lastIndex));
               //
            }
            else{
                bitMask.set((startIndex + 1), lastIndex, false);
            }

            logger.info("dmax = " + dmax);
        }

        return bitMask;
    }

    public static List<Point> reduce(List<Point> points, double epsilon, final double xScaling, final double yScaling) {
        List<Point> scaledPoints = points.stream().map(p-> new Point(p.x * xScaling, p.y * yScaling)).collect(Collectors.toList());
        BitSet bitMask = getReductionMask(scaledPoints, epsilon);
        List<Point> resList = new ArrayList<>();

        for (int i = 0, n = points.size(); i < n; ++i) {
            if (bitMask.get(i)) {
                resList.add(points.get(i));
            }
        }
        return resList;
    }


    private static double perpendicularDistance(Point p, Point a, Point b) {
        if (a.equals(b)){
            return p.distance(a);
        }
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
