[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.4446960.svg)](https://doi.org/10.5281/zenodo.4446960)

About VideoHandler
------------------
VideoHandler is an [Fiji](https://fiji.sc) plugin, which uses [JavaCV](https://github.com/bytedeco/javacv) to provide video loading functionality.  Videos can be loaded both directly to Fiji or into a MIA workflow if the [MIA (Modular Image Analysis)](https://github.com/mianalysis/MIA) workflow automation plugin is also installed.

Installation
------------
1. Download the latest version of the plugin from the [Releases](https://github.com/mianalysis/VideoHandler/releases) page.  VideoHandler is platform-specific, so please only download the .jar file corresponding to your system (e.g. for a 64-bit Windows computer, use the "win64" version)
2. Place this .jar file into the /plugins directory of the your Fiji installation

Usage
-----
To load videos directly into Fiji:
1. Within Fiji, go to File > Import > Video (JavaCV)...
2. Select video file to load
3. Specify frames and channels to load, then click "OK".  Indices can be written in the following forms:
    - "15-70" using hyphens to specify a continuous range
    - "5-20-4" using hypens to specify every nth index in a range ("5-20-4" specifies indices 5,9,13 and 17)
    - "5,10,15,20,25" as comma-separated lists
    - "100-end" using "end" keyword to specify maximum available index (e.g. for videos of unknown length)

To load videos into MIA:
1. Add the VideoHandler module to a MIA workflow from + > Input output > Load video
2. The "Load video" parameters page allows the channel and frame ranges to be specified in the same form as described above (for Fiji loading).  
3. (Optional) Use the "Crop mode" parameter to load a subset of the video in XY
4. (Optional) Use the "Set manual spatial calibration" parameter to specify XY spatial calibration

Acknowledgements
----------------
This plugin relies on the [JavaCV](https://github.com/bytedeco/javacv) library, which comes bundled with each platform-specific [release](https://github.com/mianalysis/VideoHandler/releases).

Note
----
This plugin is still in development and test coverage is currently incomplete.  Please keep an eye on results and add an [issue](https://github.com/mianalysis/VideoHandler/issues) if any problems are encountered.
