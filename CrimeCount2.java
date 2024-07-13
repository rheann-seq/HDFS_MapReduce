import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class CrimeCount2 {

    public static class CrimeMapper extends Mapper<LongWritable, Text, Text, Text> {

        private Text outputKey = new Text();
        private Text outputValue = new Text();

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] parts = line.split("\t");

            // Skip header row
            if (parts[0].equalsIgnoreCase("X") || parts[1].equalsIgnoreCase("Y")) return;

            // Assuming the X and Y coordinates are in the first two columns
            double crimeX = Double.parseDouble(parts[0]);
            double crimeY = Double.parseDouble(parts[1]);

            // Create a KML Placemark element for the crime
            StringBuilder kml = new StringBuilder();
            kml.append("<Placemark>\n");
            kml.append("  <name>Crime</name>\n");
            kml.append("  <description>Crime Location</description>\n");
            kml.append("  <Point>\n");
            kml.append("    <coordinates>").append(crimeX).append(",").append(crimeY).append(",0</coordinates>\n");
            kml.append("  </Point>\n");
            kml.append("</Placemark>\n");

            outputKey.set("");
            outputValue.set(kml.toString());
            context.write(outputKey, outputValue);
        }
    }

    public static class CrimeReducer extends Reducer<Text, Text, Text, Text> {

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {
            StringBuilder kml = new StringBuilder();
            kml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            kml.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n<Document>\n");

            for (Text location : values) {
                kml.append("<Placemark>\n<Point>\n");
                kml.append("<coordinates>").append(location.toString()).append("</coordinates>\n");
                kml.append("</Point>\n</Placemark>\n");
            }

            kml.append("</Document>\n</kml>");
            context.write(null, new Text(kml.toString()));
            /*context.write(null, new Text("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                    "<Document>\n"));

            for (Text value : values) {
                context.write(null, value);
            }

            context.write(null, new Text("</Document>\n" +
                    "</kml>\n"));*/
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "crime count");

        job.setJarByClass(CrimeCount2.class);
        job.setMapperClass(CrimeMapper.class);
        job.setReducerClass(CrimeReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
