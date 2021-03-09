package wbif.sjx.VideoHandler;

import fiji.stacks.Hyperstack_rearranger;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.ChannelSplitter;
import ij.plugin.HyperStackConverter;
import ij.process.ImageProcessor;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.drew.lang.annotations.Nullable;


public class VideoLoaderCore {
    public static ImagePlus getVideo(String path, String frameRange, String channelRange, @Nullable int[] crop) throws FrameGrabber.Exception, FileNotFoundException {
        String outputName = new File(path).getName();

        // Initialising the video loader and converter
        Java2DFrameConverter frameConverter = new Java2DFrameConverter();
        FFmpegFrameGrabber loader = new FFmpegFrameGrabber(path);
        loader.start();

        // Getting an ordered list of frames to be imported
        int[] framesList = interpretIntegers(frameRange,true);
        if (framesList[framesList.length-1] == Integer.MAX_VALUE) framesList = extendRangeToEnd(framesList,loader.getLengthInFrames());
        TreeSet<Integer> frames = Arrays.stream(framesList).boxed().collect(Collectors.toCollection(TreeSet::new));

        int[] channelsList = interpretIntegers(channelRange,true);
        if (channelsList[channelsList.length-1] == Integer.MAX_VALUE) channelsList = extendRangeToEnd(channelsList,loader.getPixelFormat());
        TreeSet<Integer> channels = Arrays.stream(channelsList).boxed().collect(Collectors.toCollection(TreeSet::new));

        int left = 0;
        int top = 0;
        int origWidth = loader.getImageWidth();
        int origHeight = loader.getImageHeight();
        int width = origWidth;
        int height = origHeight;

        if (crop != null) {
            left = crop[0];
            top = crop[1];
            width = crop[2];
            height = crop[3];
        }

        ImagePlus ipl = IJ.createHyperStack(outputName,width, height,channelsList.length,1,framesList.length,8);
        int count = 1;
        int total = frames.size();
        IJ.showStatus("Loading video");
        for (int frame:frames) {
            IJ.showProgress(((double) count)/((double) total));

            loader.setVideoFrameNumber(frame-1);

            ImagePlus frameIpl = new ImagePlus("Temporary",frameConverter.convert(loader.grabImage()));

            for (int channel:channels) {
                ipl.setPosition(channel,1,count);

                ImageProcessor ipr = ChannelSplitter.getChannel(frameIpl,channel).getProcessor(1);
                if (crop != null) {
                    ipr.setRoi(left,top,width,height);
                    ipr = ipr.crop();
                }

                ipl.setProcessor(ipr);

            }

            // for (int channel:channels) {
            //     int frameIdx = frameIpl.getStackIndex(channel, 1, 1);
            //     int iplIdx = ipl.getStackIndex(channel, 1, count);
            //     ImageProcessor frameIpr = frameIpl.getStack().getProcessor(frameIdx);
                

            //     if (crop != null) {
            //         frameIpr.setRoi(left,top,width,height);
            //         frameIpr = frameIpr.crop();
            //     }

            //     ipl.getStack().setProcessor(frameIpr,iplIdx);

            // }

            count++;

        }

        // This will probably load as a Z-stack rather than timeseries, so convert it to a stack
        if (((ipl.getNFrames() == 1 && ipl.getNSlices() > 1) || (ipl.getNSlices() == 1 && ipl.getNFrames() > 1) )) {
            convertToTimeseries(ipl);
            ipl.getCalibration().pixelDepth = 1;
        }

        ipl.setPosition(1);
        ipl.updateChannelAndDraw();

        // Closing the loader
        loader.close();

        return ipl;

    }

    public static void convertToTimeseries(ImagePlus inputImagePlus) {
        int nChannels = inputImagePlus.getNChannels();
        int nFrames = inputImagePlus.getNFrames();
        int nSlices = inputImagePlus.getNSlices();
        if (inputImagePlus.getNSlices() != 1 || inputImagePlus.getNFrames() <= 1) {
            ImagePlus processedImagePlus = HyperStackConverter.toHyperStack(inputImagePlus, nChannels, nFrames, nSlices);
            processedImagePlus = Hyperstack_rearranger.reorderHyperstack(processedImagePlus, "CTZ", true, false);
            inputImagePlus.setStack(processedImagePlus.getStack());
        }
    }

    public static int[] extendRangeToEnd(int[] inputRange, int end) {
        TreeSet<Integer> values = new TreeSet<>();

        int start;
        for(start = 0; start < inputRange.length - 3; ++start) {
            values.add(inputRange[start]);
        }

        start = inputRange[inputRange.length - 3];
        int interval = inputRange[inputRange.length - 2] - start;

        for(int i = start; i <= end; i += interval) {
            values.add(i);
        }

        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    public static int[] interpretIntegers(String range, boolean ascendingOrder) {
        // Creating a TreeSet to store the indices we've collected.  This will order numerically and remove duplicates.
        LinkedHashSet<Integer> values = new LinkedHashSet<>();

        // Removing white space
        range = range.replaceAll("\\s","");

        // Setting patterns for ranges and values
        Pattern singleRangePattern = Pattern.compile("^([-]?[\\d]+)-([-]?[\\d]+)$");
        Pattern singleRangeEndPattern = Pattern.compile("^([-]?[\\d]+)-end$");
        Pattern intervalRangePattern = Pattern.compile("^([-]?[\\d]+)-([-]?[\\d]+)-([-]?[\\d]+)$");
        Pattern intervalRangeEndPattern = Pattern.compile("^([-]?[\\d]+)-end-([-]?[\\d]+)$");
        Pattern singleValuePattern = Pattern.compile("^[-]?[\\d]+$");

        // First, splitting comma-delimited sections
        StringTokenizer stringTokenizer = new StringTokenizer(range,",");
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();

            // If it matches the single range pattern processAutomatic as a range, otherwise, check if it's a single value.
            Matcher singleRangeMatcher = singleRangePattern.matcher(token);
            Matcher singleRangeEndMatcher = singleRangeEndPattern.matcher(token);
            Matcher intervalRangeMatcher = intervalRangePattern.matcher(token);
            Matcher intervalRangeEndMatcher = intervalRangeEndPattern.matcher(token);
            Matcher singleValueMatcher = singleValuePattern.matcher(token);

            if (singleRangeMatcher.matches()) {
                int start = Integer.parseInt(singleRangeMatcher.group(1));
                int end = Integer.parseInt(singleRangeMatcher.group(2));
                int interval = (end >= start) ? 1 : -1;
                int nValues = (end-start)/interval + 1;

                for (int i=0;i<nValues;i++) {
                    values.add(start);
                    start = start + interval;
                }

            } else if (singleRangeEndMatcher.matches()) {
                // If the numbers should proceed to the end, the last three added are the starting number, the starting
                // number plus one and the maximum value
                int start = Integer.parseInt(singleRangeEndMatcher.group(1));

                values.add(start);
                values.add(start+1);
                values.add(Integer.MAX_VALUE);

            } else if (intervalRangeMatcher.matches()) {
                int start = Integer.parseInt(intervalRangeMatcher.group(1));
                int end = Integer.parseInt(intervalRangeMatcher.group(2));
                int interval = Integer.parseInt(intervalRangeMatcher.group(3));
                int nValues = (end-start)/interval + 1;

                for (int i=0;i<nValues;i++) {
                    values.add(start);
                    start = start + interval;
                }

            } else if (intervalRangeEndMatcher.matches()) {
                // If the numbers should proceed to the end, the last three added are the starting number, the starting
                // number plus the interval and the maximum value
                int start = Integer.parseInt(intervalRangeEndMatcher.group(1));
                int interval = Integer.parseInt(intervalRangeEndMatcher.group(2));

                values.add(start);
                values.add(start+interval);
                values.add(Integer.MAX_VALUE);

            } else if (singleValueMatcher.matches()) {
                values.add(Integer.parseInt(token));

            }
        }

        // Returning an array of the indices.  If they should be in ascending order, put them in a TreeSet first
        if (ascendingOrder) return new TreeSet<>(values).stream().mapToInt(Integer::intValue).toArray();
        else return values.stream().mapToInt(Integer::intValue).toArray();

    }
}
