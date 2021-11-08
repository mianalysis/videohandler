package io.github.mianalysis.videohandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.TreeSet;
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
import io.github.mianalysis.mia.object.units.TemporalUnit;
import io.github.mianalysis.videohandler.VideoLoader.ScaleModes;
import ome.units.UNITS;
import ome.units.quantity.Time;
import ome.units.unit.Unit;

public class VideoLoaderCore {
    public static ImagePlus getVideo(String path, String frameRange, String channelRange, @Nullable int[] crop,
            double[] scaleFactors, String scaleMode)
            throws FrameGrabber.Exception, FileNotFoundException, FrameOutOfRangeException {
        String outputName = new File(path).getName();

        // Initialising the video loader and converter
        Java2DFrameConverter frameConverter = new Java2DFrameConverter();
        FFmpegFrameGrabber loader = new FFmpegFrameGrabber(path);
        loader.start();

        // Getting an ordered list of frames to be imported
        int[] framesList = CommaSeparatedStringInterpreter.interpretIntegers(frameRange, true,loader.getLengthInFrames());
        int maxFrames = loader.getLengthInFrames();
        if (framesList[framesList.length - 1] > maxFrames) {
            loader.close();
            throw new FrameOutOfRangeException("Specified frame range (" + framesList[0] + "-"
                    + framesList[framesList.length - 1] + ") exceeds video length (" + maxFrames + " frames).");
        }
        TreeSet<Integer> frames = Arrays.stream(framesList).boxed().collect(Collectors.toCollection(TreeSet::new));

        int[] channelsList = CommaSeparatedStringInterpreter.interpretIntegers(channelRange, true,loader.getPixelFormat());
        TreeSet<Integer> channels = Arrays.stream(channelsList).boxed().collect(Collectors.toCollection(TreeSet::new));

        int left = 0;
        int top = 0;
        int width = loader.getImageWidth();
        int height = loader.getImageHeight();

        if (crop != null) {
            left = crop[0];
            top = crop[1];
            width = crop[2];
            height = crop[3];
        }

        int widthOut = width;
        int heightOut = height;

        // Applying scaling
        switch (scaleMode) {
        case ScaleModes.NONE:
            scaleFactors[0] = 1;
            scaleFactors[1] = 1;
            break;
        case ScaleModes.NO_INTERPOLATION:
        case ScaleModes.BILINEAR:
        case ScaleModes.BICUBIC:
            widthOut = (int) Math.round(width * scaleFactors[0]);
            heightOut = (int) Math.round(height * scaleFactors[1]);
            break;
        }

        ImagePlus ipl = IJ.createHyperStack(outputName, widthOut, heightOut, channelsList.length, 1, framesList.length,
                8);
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

                // Applying scaling
                switch (scaleMode) {
                case ScaleModes.NO_INTERPOLATION:
                    ipr.setInterpolationMethod(ImageProcessor.NONE);
                    ipr = ipr.resize(widthOut, heightOut);
                    break;
                case ScaleModes.BILINEAR:
                    ipr.setInterpolationMethod(ImageProcessor.BILINEAR);
                    ipr = ipr.resize(widthOut, heightOut);
                    break;
                case ScaleModes.BICUBIC:
                    ipr.setInterpolationMethod(ImageProcessor.BICUBIC);
                    ipr = ipr.resize(widthOut, heightOut);
                    break;
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
        setTemporalCalibration(ipl, fps);

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
