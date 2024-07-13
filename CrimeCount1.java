import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class CrimeCount1 {

    public static class CrimeMapper extends Mapper<LongWritable, Text, Text, IntWritable> {

        private final static IntWritable one = new IntWritable(1);
        private Text offenseType = new Text();

        // Target coordinates of 3803 Forbes Avenue in Oakland
        private static final double TARGET_X = 1354326.897;
        private static final double TARGET_Y = 411447.7828;
        private static final double DISTANCE_THRESHOLD = 350.0; // in meters

        // Convert feet to meters
        private static final double FEET_TO_METERS = 0.3048;

        // Calculate distance using Pythagorean theorem
        private double calculateDistance(double x1, double y1, double x2, double y2) {
            double dx = (x2 - x1) * FEET_TO_METERS;
            double dy = (y2 - y1) * FEET_TO_METERS;
            return Math.sqrt(dx * dx + dy * dy);
        }

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] parts = line.split("\t");

            if(parts[0].equalsIgnoreCase("x") || parts[1].equalsIgnoreCase("y")) return;

            // Assuming the X and Y coordinates are in the first two columns
            double crimeX = Double.parseDouble(parts[0]);
            double crimeY = Double.parseDouble(parts[1]);

            // Calculate distance from target coordinates
            double distance = calculateDistance(crimeX, crimeY, TARGET_X, TARGET_Y);

            // Check if the offense is aggravated assault and within the threshold distance
            if (parts[4].equalsIgnoreCase("aggravated assault") && distance <= DISTANCE_THRESHOLD) {
                offenseType.set("aggravated assault within 350m of 3803 Forbes Avenue");
                context.write(offenseType, one);
            }
        }
    }

    public static class CrimeReducer extends Reducer<Text, IntWritable, Text, IntWritable> {

        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable value : values) {
                sum += value.get();
            }
            context.write(key, new IntWritable(sum));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "crime count");

        job.setJarByClass(CrimeCount1.class);
        job.setMapperClass(CrimeMapper.class);
        job.setReducerClass(CrimeReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
