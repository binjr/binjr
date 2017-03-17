package jrds;

import eu.fthevenet.util.text.BinaryPrefixFormat;
import eu.fthevenet.util.text.MetricPrefixFormat;
import eu.fthevenet.util.text.PrefixFormat;

import java.text.DecimalFormat;

/**
 * Created by FTT2 on 17/03/2017.
 */
public class SuffixFormatTests {
    public static void main(String[] args) {
        long[] numbers = new long[50];
        for (int i = 0; i < numbers.length; i++)
            numbers[i] = Math.random() < 0.5 ? (long) (Math.random() * Short.MAX_VALUE) : (long) (Math.random() * Short.MIN_VALUE);
        System.out.println(convert(new MetricPrefixFormat(), numbers) );
        System.out.println(convert(new BinaryPrefixFormat(), numbers) );



        double[] doubles = new double[50];
        for (int i = 0; i < numbers.length; i++)
            doubles[i] = Math.random() < 0.5 ? (double) (Math.random() * Short.MAX_VALUE) : (double) (Math.random() * Short.MIN_VALUE);

        System.out.println(convert(new MetricPrefixFormat(), doubles) );
        System.out.println(convert(new BinaryPrefixFormat(), doubles) );
    }


    private static long convert(PrefixFormat format, double[] numbers) {
        DecimalFormat formatter = new DecimalFormat("###,###.##");
        long l = System.currentTimeMillis();
        for (int i = 0; i < numbers.length; i++) {
            System.out.println("original = " + formatter.format(numbers[i]) + " formatted = " + format.format(numbers[i]));
        }
        return System.currentTimeMillis() - l;
    }

    private static long convert(PrefixFormat format, long[] numbers) {
        long l = System.currentTimeMillis();
        for (int i = 0; i < numbers.length; i++) {
            System.out.println("original = " + numbers[i] + " formatted = " + format.format(numbers[i]));
        }
        return System.currentTimeMillis() - l;
    }


}
