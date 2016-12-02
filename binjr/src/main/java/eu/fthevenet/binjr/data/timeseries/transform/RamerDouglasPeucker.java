package eu.fthevenet.binjr.data.timeseries.transform;

import javafx.geometry.Point2D;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Stack;

/**
 * Created by FTT2 on 02/11/2016.
 */
public class RamerDouglasPeucker {

    private static final Logger logger = LogManager.getLogger(RamerDouglasPeucker.class);
    private static int maxReductionIteration = 100;

    public static List<Point2D> simplify(List<Point2D> points, double epsilon) {
        if (points.size() < 2) {
            return points;
        }
        List<Point2D> resultList = new ArrayList<>();
        double dmax = -1.0;
        int index = 0;
        int end = points.size() - 1;
        for (int i = 1; i < end - 1; i++) {
            double d = perpendicularDistance(points.get(i), points.get(0), points.get(end));
            if (d > dmax) {
                dmax = d;
                index = i;
            }
        }
        if (dmax > epsilon) {
            List<Point2D> res1 = simplify(points.subList(0, index), epsilon);
            List<Point2D> res2 = simplify(points.subList(index, end + 1), epsilon);
            resultList.addAll(res1);
            resultList.addAll(res2);
        }
        else {
            resultList.add(points.get(0));
            resultList.add(points.get(end));
        }
        return resultList;
    }


    private static BitSet getReductionMask(List<Point2D> points, double epsilon) {
        Stack<Pair<Integer, Integer>> stk = new Stack<>();
        int startIndex = 0;
        int lastIndex = points.size() - 1;
        stk.push(new Pair<>(startIndex, lastIndex));
        BitSet bitMask = new BitSet(lastIndex + 1);
        bitMask.set(0, lastIndex + 1, true);
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
            }
            else {
                bitMask.set((startIndex + 1), lastIndex, false);
            }
        }
        return bitMask;
    }

    public static List<Point2D> reduce(List<Point2D> points, int threshold) {
        long nbPointAfterRDP = 0;
        List<Point2D> scaledPoints = points; //points.stream().map(p -> new Point2D(p.getX() * xScaling, p.getY() * yScaling)).collect(Collectors.toList());

        double epsilon = avg(deviations(scaledPoints)) / 5.0;
        logger.debug("avg deviation = " + epsilon);
        List<Point2D> resList = new ArrayList<>(points);
        for (int i = 0;  i < maxReductionIteration && resList.size() > threshold; i++) {
            logger.trace("iteration=" + i + " epsilon=" + epsilon + " points=" +points.size());
            resList = simplify(points, epsilon);
            epsilon *=1.1;
            logger.trace("After reduction:"+ resList.size());
        }
        return resList;
    }


    private static double perpendicularDistance(Point2D p, Point2D a, Point2D b) {
        if (a.equals(b)) {
            return p.distance(a);
        }
        double n = Math.abs((b.getX() - a.getX()) * (a.getY() - p.getY()) - (a.getX() - p.getX()) * (b.getY() - a.getY()));
        double d = Math.sqrt((b.getX() - a.getX()) * (b.getX() - a.getX()) + (b.getY() - a.getY()) * (b.getY() - a.getY()));
        return n / d;
    }


    /**
     * For each 3 consecutive points in the list this function calculates the distance
     * from the middle point to a line defined by the first and third point.
     * <p>
     * The result may be used to find a proper epsilon by calculating
     * maximum {@link #max(double[])} or average {@link #avg(double[])} from
     * all deviations.
     */
    public static <P extends Point2D> double[] deviations(List<P> points) {
        double[] deviations = new double[Math.max(0, points.size() - 2)];
        for (int i = 2; i < points.size(); i++) {
            P p1 = points.get(i - 2);
            P p2 = points.get(i - 1);
            P p3 = points.get(i);
            double dev = perpendicularDistance(p2, p1, p3);// new Line<P>(p1, p3).distance(p2);
            deviations[i - 2] = dev;
        }
        return deviations;
    }

    public static double sum(double[] values) {
        double sum = 0.0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i];
        }
        return sum;
    }


    public static double avg(double[] values) {
        if (values.length > 0) {
            return sum(values) / values.length;
        }
        else {
            return 0.0;
        }
    }

    public static double max(double[] values) {
        double max = 0.0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }

}
