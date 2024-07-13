import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class LetterCounter extends Configured implements Tool {

    public static class LetterCountMap extends Mapper<LongWritable, Text, Text, IntWritable> {
        private final static IntWritable one = new IntWritable(1);
        private Text letter = new Text();

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            for (char c : line.toCharArray()) {
                if (Character.isLetter(c)) {
                    letter.set(Character.toString(Character.toLowerCase(c)));
                    context.write(letter, one);
                }
            }
        }
    }

    public static class LetterCountReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private Map<Text, IntWritable> countMap = new HashMap<>();

        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable value : values) {
                sum += value.get();
            }
            countMap.put(new Text(key), new IntWritable(sum));
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            TreeMap<Text, IntWritable> sortedMap = new TreeMap<>(new LetterComparator(countMap));
            sortedMap.putAll(countMap);

            for (Map.Entry<Text, IntWritable> entry : sortedMap.entrySet()) {
                context.write(entry.getKey(), entry.getValue());
            }
        }
    }

    private static class LetterComparator implements Comparator<Text> {
        Map<Text, IntWritable> base;

        public LetterComparator(Map<Text, IntWritable> base) {
            this.base = base;
        }

        // Note: this comparator imposes orderings that are inconsistent with equals.
        @Override
        public int compare(Text a, Text b) {
            int diff = base.get(b).get() - base.get(a).get();
            if (diff != 0) {
                return diff;
            } else {
                return a.compareTo(b);
            }
        }
    }

    public int run(String[] args) throws Exception {

        Job job = new Job(getConf());
        job.setJarByClass(LetterCounter.class);
        job.setJobName("lettercount");

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        job.setMapperClass(LetterCountMap.class);
        job.setReducerClass(LetterCountReducer.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        boolean success = job.waitForCompletion(true);
        return success ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        int result = ToolRunner.run(new Configuration(), new LetterCounter(), args);
        System.exit(result);
    }

}
