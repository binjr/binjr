package eu.fthevenet.util.text;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Created by FTT2 on 17/03/2017.
 */
public abstract class PrefixFormat {
    public static final String PATTERN = "###,###.##";
    protected final int base;
    private final NavigableMap<Long, String> longSuffixes = new TreeMap<>();
    private final NavigableMap<Double, String> doubleSuffixes = new TreeMap<>();


    public PrefixFormat(int base, String[] suffixes) {
        this.base = base;
        for (int i = 0; i < suffixes.length; i++) {
            this.longSuffixes.put(pow(base, i+1), suffixes[i]);
            this.doubleSuffixes.put(Math.pow(base, i+1), suffixes[i]);
        }
    }

    private long pow(long a, int b) {
        if (b == 0) {
            return 1;
        }
        if (b == 1) {
            return a;
        }
        if (b % 2 == 0) {
            return pow(a * a, b / 2); //even a=(a^2)^b/2
        }
        else {
            return a * pow(a * a, b / 2); //odd  a=a*(a^2)^b/2
        }

    }

    public String format(long value) {
        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if (value == Long.MIN_VALUE) {
            return format(Long.MIN_VALUE + 1);
        }
        if (value < 0) {
            return "-" + format(-value);
        }
        if (value < base) {
            return Long.toString(value); //deal with easy case
        }

        Map.Entry<Long, String> e = longSuffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10); //the number part of the output times 10
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }

    public String format(double value) {
        DecimalFormat formatter = new DecimalFormat(PATTERN);
        if (Double.isNaN(value)){
            return "NaN";
        }
        if (Double.isInfinite(value)){
            return "Infinite";
        }

        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if (value == Long.MIN_VALUE) {
            return format(Long.MIN_VALUE + 1);
        }
        if (value < 0) {
            return "-" + format(-value);
        }
        if (value < base) {
            return formatter.format(value); //deal with easy case
        }

        Map.Entry<Double, String> e = doubleSuffixes.floorEntry(value);
        Double divideBy = e.getKey();
        String suffix = e.getValue();


        double truncated = value / (divideBy / 10); //the number part of the output times 10
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        truncated =  hasDecimal ? (truncated / 10) : (truncated / 10d);
        return formatter.format(truncated) + suffix;
    }


//    public  String format(long number) {
//        String ret;
//        if (number >= 0) {
//            ret = "";
//        }else {
//            ret = "-";
//            number = -number;
//        }
//        if (number < base)
//            return ret + number;
//        for (int i = 0; ; i++) {
//            if (number < base*10 && number % base >= base/10)
//                return ret + (number / base) + '.' + ((number % base) / base/10) + longSuffixes[i];
//            number /= base;
//            if (number < base)
//                return ret + number + longSuffixes[i];
//        }
//    }


}
