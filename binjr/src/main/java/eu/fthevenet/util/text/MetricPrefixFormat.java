package eu.fthevenet.util.text;

/**
 * Created by FTT2 on 17/03/2017.
 */
public class MetricPrefixFormat extends PrefixFormat {

    public MetricPrefixFormat() {
        super(1000,new String[] {"k", "M", "G", "T", "P", "E"});


    }





//    @Override
//    public  String format(long value) {
//        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
//        if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1);
//        if (value < 0) return "-" + format(-value);
//        if (value < base) return Long.toString(value); //deal with easy case
//
//        Map.Entry<Long, String> e = suffixes.floorEntry(value);
//        Long divideBy = e.getKey();
//        String suffix = e.getValue();
//
//        long truncated = value / (divideBy / 10); //the number part of the output times 10
//        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
//        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
//    }
}
