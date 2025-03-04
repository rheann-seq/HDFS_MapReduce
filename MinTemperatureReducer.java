//package org.myorg;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import java.io.IOException;
import java.util.Iterator;

public class MinTemperatureReducer extends MapReduceBase implements Reducer<Text, IntWritable, Text, IntWritable> {
    public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text,
            IntWritable> output, Reporter reporter) throws IOException {

        // from the list of values, find the maximum
        int minValue = Integer.MAX_VALUE;
        while (values.hasNext()) {
            minValue = Math.min(minValue, values.next().get());
        }
        // emit (key = year, value = maxTemp = max for year)
        output.collect(key, new IntWritable(minValue));
    }
}
