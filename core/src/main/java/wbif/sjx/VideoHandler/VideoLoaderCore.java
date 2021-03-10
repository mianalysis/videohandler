package wbif.sjx.VideoHandler;

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

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import fiji.stacks.Hyperstack_rearranger;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import ij.plugin.HyperStackConverter;
import ij.process.ImageProcessor;
import ome.units.UNITS;
import ome.units.quantity.Time;
import ome.units.unit.Unit;
import wbif.sjx.MIA.Object.Units.TemporalUnit;
import wbif.sjx.MIA.Process.CommaSeparatedStringInterpreter;

public class VideoLoaderCore {
    public static ImagePlus getVideo(String path, String frameRange, String channelRange, @Nullable int[] crop)
            throws FrameGrabber.Exception, FileNotFoundException, FrameOutOfRangeException {
        String outputName = new File(path).getName();

        // Initialising the video loader and converter
        Java2DFrameConverter frameConverter = new Java2DFrameConverter();
        FFmpegFrameGrabber loader = new FFmpegFrameGrabber(path);
        loader.start();

        // Getting an ordered list of frames to be imported
        int[] framesList = CommaSeparatedStringInterpreter.interpretIntegers(frameRange, true);
        int maxFrames = loader.getLengthInFrames();
        if (framesList[framesList.length - 1] == Integer.MAX_VALUE)
            framesList = extendRangeToEnd(framesList, maxFrames);
        if (framesList[framesList.length - 1] > maxFrames) {
            loader.close();
            throw new FrameOutOfRangeException("Specified frame range ("+framesList[0]+"-"+framesList[framesList.length-1]+") exceeds video length ("+maxFrames+" frames).");
        }
        TreeSet<Integer> frames = Arrays.stream(framesList).boxed().collect(Collectors.toCollection(TreeSet::new));

        int[] channelsList = CommaSeparatedStringInterpreter.interpretIntegers(channelRange, true);
        if (channelsList[channelsList.length - 1] == Integer.MAX_VALUE)
            channelsList = extendRangeToEnd(channelsList, loader.getPixelFormat());
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

        ImagePlus ipl = IJ.createHyperStack(outputName, width, height, channelsList.length, 1, framesList.length, 8);
        int count = 1;
        int total = frames.size();
        IJ.showStatus("Loading video");
        for (int frame : frames) {
            IJ.showProgress(((double) count) / ((double) total));

            loader.setVideoFrameNumber(frame - 1);

            ImagePlus frameIpl = new ImagePlus("Temporary", frameConverter.convert(loader.grabImage()));

            for (int channel : channels) {
                ipl.setPosition(channel, 1, count);

                ImageProcessor ipr = ChannelSplitter.getChannel(frameIpl, channel).getProcessor(1);
                if (crop != null) {
                    ipr.setRoi(left, top, width, height);
                    ipr = ipr.crop();
                }

                ipl.setProcessor(ipr);

            }

            // for (int channel:channels) {
            // int frameIdx = frameIpl.getStackIndex(channel, 1, 1);
            // int iplIdx = ipl.getStackIndex(channel, 1, count);
            // ImageProcessor frameIpr = frameIpl.getStack().getProcessor(frameIdx);

            // if (crop != null) {
            // frameIpr.setRoi(left,top,width,height);
            // frameIpr = frameIpr.crop();
            // }

            // ipl.getStack().setProcessor(frameIpr,iplIdx);

            // }

            count++;

        }

        // This will probably load as a Z-stack rather than timeseries, so convert it to
        // a stack
        if (((ipl.getNFrames() == 1 && ipl.getNSlices() > 1) || (ipl.getNSlices() == 1 && ipl.getNFrames() > 1))) {
            convertToTimeseries(ipl);
            ipl.getCalibration().pixelDepth = 1;
        }

        double fps = loader.getFrameRate();
        setTemporalCalibration(ipl,fps);

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
            ImagePlus processedImagePlus = HyperStackConverter.toHyperStack(inputImagePlus, nChannels, nFrames,
                    nSlices);
            processedImagePlus = Hyperstack_rearranger.reorderHyperstack(processedImagePlus, "CTZ", true, false);
            inputImagePlus.setStack(processedImagePlus.getStack());
        }
    }

    public static int[] extendRangeToEnd(int[] inputRange, int end) {
        TreeSet<Integer> values = new TreeSet<>();

        int start;
        for (start = 0; start < inputRange.length - 3; ++start) {
            values.add(inputRange[start]);
        }

        start = inputRange[inputRange.length - 3];
        int interval = inputRange[inputRange.length - 2] - start;

        for (int i = start; i <= end; i += interval) {
            values.add(i);
        }

        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    public static void setTemporalCalibration(ImagePlus ipl, double fps) {        
        Unit<Time> temporalUnits = TemporalUnit.getOMEUnit();

        Calibration cal = ipl.getCalibration();
        cal.setTimeUnit(temporalUnits.getSymbol());
        cal.fps = fps;
        cal.frameInterval = UNITS.SECOND.convertValue(1 / fps, temporalUnits);

    }
}
