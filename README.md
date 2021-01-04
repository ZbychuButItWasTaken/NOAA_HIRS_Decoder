# NOAA_HIRS_Decoder
### Decoder for decoding HIRS data From NOAA satellites

## Website
Check out the project website for a tutorial: https://noaa_hirs_decoder.surge.sh
You can join our discord server: https://discord.gg/Ct2vzGK

# Features
 - Decode HIRS data to pictures
 - Histogram equalization
 - Multispectral analysis with brightness values
 - Composite of all channels
 - Option to use a common folder
 - Do not skip missing minorframes but black them out in the image
 - Save the time of the pass
 - Equalize bad/missing pixels with the neighboring ones
 - Custom sized composites
 - Composite of equalized images
 - Open file as an argument for application
  - Build RGB images from 3 channels
  - Add support for different color palettes
  - Get the satellites name
  - Cut off bad start and end

# TODO
 - GUI
 - ~~Cloud top height approximation~~ Isn't possible because of the degraded performance of HIRS instrument
 - Add MetOp's HIRS support and possibly even FY's IRAS

# Additional info
This program uses and includes following libraries: ini4j, Apache POI, openjfx, launch4j and JCommander.

