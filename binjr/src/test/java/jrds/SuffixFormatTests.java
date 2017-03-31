package jrds;

import eu.fthevenet.util.text.BinaryPrefixFormatter;
import eu.fthevenet.util.text.MetricPrefixFormatter;
import eu.fthevenet.util.text.PrefixFormatter;

import java.text.DecimalFormat;

/**
 * Suffix formatter tests
 *
 * @author Frederic Thevenet
 */
public class SuffixFormatTests {
    public static void main(String[] args) {
        long[] numbers = new long[50];
        for (int i = 0; i < numbers.length; i++)
            numbers[i] = Math.random() < 0.5 ? (long) (Math.random() * Short.MAX_VALUE) : (long) (Math.random() * Short.MIN_VALUE);
        System.out.println(convert(new MetricPrefixFormatter(), numbers) );
        System.out.println(convert(new BinaryPrefixFormatter(), numbers) );



        double[] doubles = new double[50];
        for (int i = 0; i < numbers.length; i++)
            doubles[i] = Math.random() < 0.5 ? Math.random() * Short.MAX_VALUE : Math.random() * Short.MIN_VALUE;

        System.out.println(convert(new MetricPrefixFormatter(), doubles) );
        System.out.println(convert(new BinaryPrefixFormatter(), doubles) );
    }


    private static long convert(PrefixFormatter format, double[] numbers) {
        DecimalFormat formatter = new DecimalFormat("###,###.##");
        long l = System.currentTimeMillis();
        for (int i = 0; i < numbers.length; i++) {
            System.out.println("original = " + formatter.format(numbers[i]) + " formatted = " + format.format(numbers[i]));
        }
        return System.currentTimeMillis() - l;
    }

    private static long convert(PrefixFormatter format, long[] numbers) {
        long l = System.currentTimeMillis();
        for (int i = 0; i < numbers.length; i++) {
            System.out.println("original = " + numbers[i] + " formatted = " + format.format(numbers[i]));
        }
        return System.currentTimeMillis() - l;
    }


}
