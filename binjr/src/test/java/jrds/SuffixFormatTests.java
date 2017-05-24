/*
 *    Copyright 2017 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

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
