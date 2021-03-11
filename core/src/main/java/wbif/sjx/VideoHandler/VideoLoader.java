package wbif.sjx.VideoHandler;

import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import ij.ImagePlus;
import ij.measure.Calibration;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import wbif.sjx.MIA.MIA;
import wbif.sjx.MIA.Module.Categories;
import wbif.sjx.MIA.Module.Category;
import wbif.sjx.MIA.Module.Module;
import wbif.sjx.MIA.Module.ModuleCollection;
import wbif.sjx.MIA.Module.InputOutput.ImageLoader;
import wbif.sjx.MIA.Object.Image;
import wbif.sjx.MIA.Object.ObjCollection;
import wbif.sjx.MIA.Object.Status;
import wbif.sjx.MIA.Object.Workspace;
import wbif.sjx.MIA.Object.Parameters.BooleanP;
import wbif.sjx.MIA.Object.Parameters.ChoiceP;
import wbif.sjx.MIA.Object.Parameters.FilePathP;
import wbif.sjx.MIA.Object.Parameters.InputImageP;
import wbif.sjx.MIA.Object.Parameters.InputObjectsP;
import wbif.sjx.MIA.Object.Parameters.OutputImageP;
import wbif.sjx.MIA.Object.Parameters.ParameterCollection;
import wbif.sjx.MIA.Object.Parameters.SeparatorP;
import wbif.sjx.MIA.Object.Parameters.Text.DoubleP;
import wbif.sjx.MIA.Object.Parameters.Text.IntegerP;
import wbif.sjx.MIA.Object.Parameters.Text.StringP;
import wbif.sjx.MIA.Object.Parameters.Text.TextAreaP;
import wbif.sjx.MIA.Object.References.Collections.ImageMeasurementRefCollection;
import wbif.sjx.MIA.Object.References.Collections.MetadataRefCollection;
import wbif.sjx.MIA.Object.References.Collections.ObjMeasurementRefCollection;
import wbif.sjx.MIA.Object.References.Collections.ParentChildRefCollection;
import wbif.sjx.MIA.Object.References.Collections.PartnerRefCollection;
import wbif.sjx.MIA.Object.Units.SpatialUnit;
import wbif.sjx.common.Object.Metadata;

public class VideoLoader extends Module {
    public static final String LOADER_SEPARATOR = "Core video loading controls";
    public static final String OUTPUT_IMAGE = "Output image";
    public static final String IMPORT_MODE = "Import mode";
    public static final String NAME_FORMAT = "Name format";
    public static final String GENERIC_FORMAT = "Generic format";
    public static final String AVAILABLE_METADATA_FIELDS = "Available metadata fields";
    public static final String PREFIX = "Prefix";
    public static final String SUFFIX = "Suffix";
    public static final String EXTENSION = "Extension";
    public static final String INCLUDE_SERIES_NUMBER = "Include series number";
    public static final String FILE_PATH = "File path";

    public static final String RANGE_SEPARATOR = "Dimension ranges and cropping";
    public static final String CHANNELS = "Channels";
    public static final String FRAMES = "Frames";
    public static final String CROP_MODE = "Crop mode";
    public static final String REFERENCE_IMAGE = "Reference image";
    public static final String LEFT = "Left coordinate";
    public static final String TOP = "Top coordinate";
    public static final String WIDTH = "Width";
    public static final String HEIGHT = "Height";
    public static final String OBJECTS_FOR_LIMITS = "Objects for limits";
    public static final String SCALE_MODE = "Scale mode";
    public static final String SCALE_FACTOR_X = "X scale factor";
    public static final String SCALE_FACTOR_Y = "Y scale factor";

    public static final String CALIBRATION_SEPARATOR = "Spatial calibration";
    public static final String SET_CAL = "Set manual spatial calibration";
    public static final String XY_CAL = "XY calibration (dist/px)";
    public static final String Z_CAL = "Z calibration (dist/px)";

    public static void main(String[] args) throws Exception {
        MIA.addPluginPackageName(VideoLoader.class.getCanonicalName());
        MIA.main(new String[] {});

    }

    public VideoLoader(ModuleCollection modules) {
        super("Load video", modules);
    }

    public interface ImportModes {
        String CURRENT_FILE = "Current file";
        String MATCHING_FORMAT = "Matching format";
        String SPECIFIC_FILE = "Specific file";

        String[] ALL = new String[] { CURRENT_FILE, MATCHING_FORMAT, SPECIFIC_FILE };

    }

    public interface NameFormats {
        String GENERIC = "Generic (from metadata)";
        String INPUT_FILE_PREFIX = "Input filename with prefix";
        String INPUT_FILE_SUFFIX = "Input filename with suffix";

        String[] ALL = new String[] { GENERIC, INPUT_FILE_PREFIX, INPUT_FILE_SUFFIX };

    }

    public interface CropModes {
        String NONE = "None";
        String FIXED = "Fixed";
        String FROM_REFERENCE = "From reference";
        String OBJECT_COLLECTION_LIMITS = "Object collection limits";

        String[] ALL = new String[] { NONE, FIXED, FROM_REFERENCE, OBJECT_COLLECTION_LIMITS };

    }

    public interface ScaleModes {
        String NONE = "No scaling";
        String NO_INTERPOLATION = "Scaling (no interpolation)";
        String BILINEAR = "Scaling (bilinear)";
        String BICUBIC = "Scaling (bicubic)";

        String[] ALL = new String[] { NONE, NO_INTERPOLATION, BILINEAR, BICUBIC };

    }

    public interface Measurements {
        String ROI_LEFT = "VIDEO_LOADING // ROI_LEFT (PX)";
        String ROI_TOP = "VIDEO_LOADING // ROI_TOP (PX)";
        String ROI_WIDTH = "VIDEO_LOADING // ROI_WIDTH (PX)";
        String ROI_HEIGHT = "VIDEO_LOADING // ROI_HEIGHT (PX)";

    }

    public String getGenericName(Metadata metadata, String genericFormat) {
        String absolutePath = metadata.getFile().getAbsolutePath();
        String path = FilenameUtils.getFullPath(absolutePath);
        String filename;
        try {
            filename = ImageLoader.getGenericName(metadata, genericFormat);
            return path + filename;
        } catch (ServiceException | DependencyException | FormatException | IOException e) {
            MIA.log.writeWarning("Can't determine filename format");
            return null;
        }
    }

    public String getPrefixName(Metadata metadata, boolean includeSeries, String ext) {
        String absolutePath = metadata.getFile().getAbsolutePath();
        String path = FilenameUtils.getFullPath(absolutePath);
        String name = FilenameUtils.removeExtension(FilenameUtils.getName(absolutePath));
        String comment = metadata.getComment();
        String series = includeSeries ? "_S" + metadata.getSeriesNumber() : "";

        return path + comment + name + series + "." + ext;

    }

    public String getSuffixName(Metadata metadata, boolean includeSeries, String ext) {
        String absolutePath = metadata.getFile().getAbsolutePath();
        String path = FilenameUtils.getFullPath(absolutePath);
        String name = FilenameUtils.removeExtension(FilenameUtils.getName(absolutePath));
        String comment = metadata.getComment();
        String series = includeSeries ? "_S" + metadata.getSeriesNumber() : "";

        return path + name + series + comment + "." + ext;

    }

    @Override
    public Category getCategory() {
        return Categories.INPUT_OUTPUT;
    }

    @Override
    public String getDescription() {
        return "Uses JavaCV to import videos.";
    }

    @Override
    public Status process(Workspace workspace) {
        // Getting parameters
        String outputImageName = parameters.getValue(OUTPUT_IMAGE);
        String importMode = parameters.getValue(IMPORT_MODE);
        String filePath = parameters.getValue(FILE_PATH);
        String nameFormat = parameters.getValue(NAME_FORMAT);

        String genericFormat = parameters.getValue(GENERIC_FORMAT);
        String prefix = parameters.getValue(PREFIX);
        String suffix = parameters.getValue(SUFFIX);
        String ext = parameters.getValue(EXTENSION);
        boolean includeSeriesNumber = parameters.getValue(INCLUDE_SERIES_NUMBER);
        String channelRange = parameters.getValue(CHANNELS);
        String frameRange = parameters.getValue(FRAMES);
        String cropMode = parameters.getValue(CROP_MODE);
        String referenceImageName = parameters.getValue(REFERENCE_IMAGE);
        int left = parameters.getValue(LEFT);
        int top = parameters.getValue(TOP);
        int width = parameters.getValue(WIDTH);
        int height = parameters.getValue(HEIGHT);
        String objectsForLimitsName = parameters.getValue(OBJECTS_FOR_LIMITS);
        String scaleMode = parameters.getValue(SCALE_MODE);
        double scaleFactorX = parameters.getValue(SCALE_FACTOR_X);
        double scaleFactorY = parameters.getValue(SCALE_FACTOR_Y);
        boolean setCalibration = parameters.getValue(SET_CAL);
        double xyCal = parameters.getValue(XY_CAL);
        double zCal = parameters.getValue(Z_CAL);

        int[] crop = null;
        switch (cropMode) {
        case CropModes.FIXED:
            crop = new int[] { left, top, width, height };
            break;
        case CropModes.FROM_REFERENCE:
            // Displaying the image
            Image referenceImage = workspace.getImage(referenceImageName);
            crop = ImageLoader.getCropROI(referenceImage);
            break;
        case CropModes.OBJECT_COLLECTION_LIMITS:
            ObjCollection objectsForLimits = workspace.getObjectSet(objectsForLimitsName);
            int[][] limits = objectsForLimits.getSpatialExtents();
            crop = new int[] {limits[0][0], limits[1][0], limits[0][1]-limits[0][0], limits[1][1]-limits[1][0]};
            break;
        }

        if (scaleMode.equals(ScaleModes.NONE)) {
            scaleFactorX = 1;
            scaleFactorY = 1;
        }
        double[] scaleFactors = new double[] { scaleFactorX, scaleFactorY };

        String pathName = null;
        switch (importMode) {
        case ImportModes.CURRENT_FILE:
            pathName = workspace.getMetadata().getFile().getAbsolutePath();
            break;

        case ImportModes.MATCHING_FORMAT:
            switch (nameFormat) {
            case NameFormats.GENERIC:
                Metadata metadata = (Metadata) workspace.getMetadata().clone();
                metadata.setComment(prefix);
                pathName = getGenericName(metadata, genericFormat);
                break;
            case NameFormats.INPUT_FILE_PREFIX:
                metadata = (Metadata) workspace.getMetadata().clone();
                metadata.setComment(prefix);
                pathName = getPrefixName(metadata, includeSeriesNumber, ext);
                break;

            case NameFormats.INPUT_FILE_SUFFIX:
                metadata = (Metadata) workspace.getMetadata().clone();
                metadata.setComment(suffix);
                pathName = getSuffixName(metadata, includeSeriesNumber, ext);
                break;
            }
            break;

        case ImportModes.SPECIFIC_FILE:
            pathName = filePath;
            break;
        }

        if (pathName == null)
            return Status.FAIL;

        Image outputImage = null;
        try {
            // First first, testing new loader
            ImagePlus outputIpl = VideoLoaderCore.getVideo(pathName, frameRange, channelRange, crop, scaleFactors, scaleMode);
            outputImage = new Image(outputImageName, outputIpl);

        } catch (FrameOutOfRangeException e1) {
            MIA.log.writeWarning(e1.getMessage());
            return Status.FAIL;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            MIA.log.writeError("Unable to read video.  Skipping this file.");
            return Status.FAIL;
        }

        // If necessary, setting the spatial calibration
        if (setCalibration) {
            writeStatus("Setting spatial calibration (XY = " + xyCal + ", Z = " + zCal + ")");
            Calibration calibration = new Calibration();

            calibration.pixelHeight = xyCal/scaleFactorX;
            calibration.pixelWidth = xyCal/scaleFactorY;
            calibration.pixelDepth = zCal;
            calibration.setUnit(SpatialUnit.getOMEUnit().getSymbol());

            outputImage.getImagePlus().setCalibration(calibration);
            outputImage.getImagePlus().updateChannelAndDraw();

        }

        // Adding image to workspace
        writeStatus("Adding image (" + outputImageName + ") to workspace");
        workspace.addImage(outputImage);

        if (showOutput)
            outputImage.showImage(outputImageName, null, false, true);

        return Status.PASS;

    }

    @Override
    protected void initialiseParameters() {
        parameters.add(new SeparatorP(LOADER_SEPARATOR, this));
        parameters.add(new OutputImageP(OUTPUT_IMAGE, this));
        parameters.add(new ChoiceP(IMPORT_MODE, this, ImportModes.CURRENT_FILE, ImportModes.ALL));
        parameters.add(new ChoiceP(NAME_FORMAT, this, NameFormats.GENERIC, NameFormats.ALL));

        parameters.add(new StringP(GENERIC_FORMAT, this));
        parameters.add(new TextAreaP(AVAILABLE_METADATA_FIELDS, this, false));
        parameters.add(new StringP(PREFIX, this));
        parameters.add(new StringP(SUFFIX, this));
        parameters.add(new StringP(EXTENSION, this));
        parameters.add(new BooleanP(INCLUDE_SERIES_NUMBER, this, true));
        parameters.add(new FilePathP(FILE_PATH, this));

        parameters.add(new SeparatorP(RANGE_SEPARATOR, this));
        parameters.add(new StringP(CHANNELS, this, "1-end"));
        parameters.add(new StringP(FRAMES, this, "1-end"));
        parameters.add(new ChoiceP(CROP_MODE, this, CropModes.NONE, CropModes.ALL,
                "Choice of loading the entire frame, or cropping in XY.<br>" + "<br>- \"" + CropModes.NONE
                        + "\" (default) will load the entire frame in XY.<br>" + "<br>- \"" + CropModes.FIXED
                        + "\" will apply a pre-defined crop to the input frame based on the parameters \"Left\", \"Top\",\"Width\" and \"Height\".<br>"
                        + "<br>- \"" + CropModes.FROM_REFERENCE
                        + "\" will display a specified image and ask the user to select a region to crop the input frame to."));
        parameters.add(new InputImageP(REFERENCE_IMAGE, this, "",
                "The frame to be displayed for selection of the cropping region if the cropping mode is set to \""
                        + CropModes.FROM_REFERENCE + "\"."));
        parameters.add(new IntegerP(LEFT, this, 0));
        parameters.add(new IntegerP(TOP, this, 0));
        parameters.add(new IntegerP(WIDTH, this, 512));
        parameters.add(new IntegerP(HEIGHT, this, 512));
        parameters.add(new InputObjectsP(OBJECTS_FOR_LIMITS, this));
        parameters.add(new ChoiceP(SCALE_MODE, this, ScaleModes.NONE, ScaleModes.ALL));
        parameters.add(new DoubleP(SCALE_FACTOR_X, this, 1));
        parameters.add(new DoubleP(SCALE_FACTOR_Y, this, 1));

        parameters.add(new SeparatorP(CALIBRATION_SEPARATOR, this));
        parameters.add(new BooleanP(SET_CAL, this, false));
        parameters.add(new DoubleP(XY_CAL, this, 1.0));
        parameters.add(new DoubleP(Z_CAL, this, 1.0));

    }

    @Override
    public ParameterCollection updateAndGetParameters() {
        ParameterCollection returnedParameters = new ParameterCollection();

        returnedParameters.add(parameters.getParameter(LOADER_SEPARATOR));
        returnedParameters.add(parameters.getParameter(OUTPUT_IMAGE));

        returnedParameters.add(parameters.getParameter(IMPORT_MODE));
        switch ((String) parameters.getValue(IMPORT_MODE)) {
        case ImageLoader.ImportModes.CURRENT_FILE:
        case ImageLoader.ImportModes.IMAGEJ:
            break;

        case ImageLoader.ImportModes.MATCHING_FORMAT:
            returnedParameters.add(parameters.getParameter(NAME_FORMAT));
            switch ((String) parameters.getValue(NAME_FORMAT)) {
            case NameFormats.GENERIC:
                returnedParameters.add(parameters.getParameter(GENERIC_FORMAT));
                returnedParameters.add(parameters.getParameter(AVAILABLE_METADATA_FIELDS));
                MetadataRefCollection metadataRefs = modules.getMetadataRefs(this);
                parameters.getParameter(AVAILABLE_METADATA_FIELDS).setValue(metadataRefs.getMetadataValues());
                break;
            case NameFormats.INPUT_FILE_PREFIX:
                returnedParameters.add(parameters.getParameter(PREFIX));
                returnedParameters.add(parameters.getParameter(INCLUDE_SERIES_NUMBER));
                returnedParameters.add(parameters.getParameter(EXTENSION));
                break;
            case NameFormats.INPUT_FILE_SUFFIX:
                returnedParameters.add(parameters.getParameter(SUFFIX));
                returnedParameters.add(parameters.getParameter(INCLUDE_SERIES_NUMBER));
                returnedParameters.add(parameters.getParameter(EXTENSION));
                break;
            }
            break;

        case ImageLoader.ImportModes.SPECIFIC_FILE:
            returnedParameters.add(parameters.getParameter(FILE_PATH));
            break;
        }

        returnedParameters.add(parameters.getParameter(RANGE_SEPARATOR));
        returnedParameters.add(parameters.getParameter(CHANNELS));
        returnedParameters.add(parameters.getParameter(FRAMES));

        returnedParameters.add(parameters.getParameter(CROP_MODE));
        switch ((String) parameters.getValue(CROP_MODE)) {
        case CropModes.FIXED:
            returnedParameters.add(parameters.getParameter(LEFT));
            returnedParameters.add(parameters.getParameter(TOP));
            returnedParameters.add(parameters.getParameter(WIDTH));
            returnedParameters.add(parameters.getParameter(HEIGHT));
            break;
        case CropModes.FROM_REFERENCE:
            returnedParameters.add(parameters.getParameter(REFERENCE_IMAGE));
            break;
        case CropModes.OBJECT_COLLECTION_LIMITS:
            returnedParameters.add(parameters.getParameter(OBJECTS_FOR_LIMITS));
            break;
        }

        returnedParameters.add(parameters.getParameter(SCALE_MODE));
            switch ((String) parameters.getValue(SCALE_MODE)) {
            case ScaleModes.NO_INTERPOLATION:
            case ScaleModes.BILINEAR:
            case ScaleModes.BICUBIC:
                returnedParameters.add(parameters.getParameter(SCALE_FACTOR_X));
                returnedParameters.add(parameters.getParameter(SCALE_FACTOR_Y));
                break;
            }

        returnedParameters.add(parameters.getParameter(CALIBRATION_SEPARATOR));
        returnedParameters.add(parameters.getParameter(SET_CAL));
        if ((boolean) parameters.getValue(SET_CAL)) {
            returnedParameters.add(parameters.getParameter(XY_CAL));
            returnedParameters.add(parameters.getParameter(Z_CAL));
        }

        return returnedParameters;

    }

    @Override
    public ImageMeasurementRefCollection updateAndGetImageMeasurementRefs() {
        ImageMeasurementRefCollection returnedRefs = new ImageMeasurementRefCollection();
        String outputImageName = parameters.getValue(OUTPUT_IMAGE);

        switch ((String) parameters.getValue(CROP_MODE)) {
        case CropModes.FROM_REFERENCE:
            returnedRefs.add(imageMeasurementRefs.getOrPut(Measurements.ROI_LEFT).setImageName(outputImageName));
            returnedRefs.add(imageMeasurementRefs.getOrPut(Measurements.ROI_TOP).setImageName(outputImageName));
            returnedRefs.add(imageMeasurementRefs.getOrPut(Measurements.ROI_WIDTH).setImageName(outputImageName));
            returnedRefs.add(imageMeasurementRefs.getOrPut(Measurements.ROI_HEIGHT).setImageName(outputImageName));

            break;
        }

        return returnedRefs;
    }

    @Override
    public ObjMeasurementRefCollection updateAndGetObjectMeasurementRefs() {
        return null;
    }

    @Override
    public MetadataRefCollection updateAndGetMetadataReferences() {
        return null;
    }

    @Override
    public boolean verify() {
        return true;
    }

    @Override
    public ParentChildRefCollection updateAndGetParentChildRefs() {
        return null;
    }

    @Override
    public PartnerRefCollection updateAndGetPartnerRefs() {
        return null;
    }
}
