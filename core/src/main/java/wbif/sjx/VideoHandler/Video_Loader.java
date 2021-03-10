package wbif.sjx.VideoHandler;

import java.io.File;

import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

public class Video_Loader implements PlugIn {
    @Override
    public void run(String path) {
        // Checking if file exists; if not, opening a file selection dialog
        if (!new File(path).exists()) {
            OpenDialog openDialog = new OpenDialog("Select video to load",null);
            path = openDialog.getDirectory()+openDialog.getFileName();
        }

        // Show parameter selection dialog
        GenericDialog genericDialog = new GenericDialog("SVG dimensions");
        genericDialog.addStringField("Frame range","1-end");
        genericDialog.addStringField("Channel range","1-end");
        genericDialog.showDialog();

        if (genericDialog.wasCanceled()) return;

        String frameRange = genericDialog.getNextString();
        String channelRange = genericDialog.getNextString();

        // Do file loading
        try {
            VideoLoaderCore.getVideo(path,frameRange,channelRange,null).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
