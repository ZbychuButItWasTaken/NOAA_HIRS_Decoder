# NOAA HIRS Decoder
Decoder for decoding HIRS data From NOAA satellites

Check out the project website: https://noaa_hirs_decoder.surge.sh

Features:
 - decode HIRS data to pictures
 - histogram equalization
 - multispectral analysis with brightness values
 - composite of all channels
 - option to use a common folder
 - Do not jump missing minorframes but black them out in the image
 - save the time of the pass
 - Equalize bad/missing pixels with the neighboring ones
 - custom sized composites
 - composite of equalized images
 - Open file as an argument for application
 - Build RGB images from 3 channels

TODO:
 - Add support for different color palettes
 - Get the satellites name
 - Cut off bad start and end
