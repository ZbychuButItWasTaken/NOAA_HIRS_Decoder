# NOAA_HIRS_Decoder
Decoder for decoding HIRS data From NOAA satellites
Features:
 - decode HIRS data to pictures
 - histogram equalization
 - multispectral analysis with brightness values
 - composite of all channels
 - option to use a common folder
 - Do not jump missing minorframes but black them out in the image
 - save the time of the pass
 - Equalize bad/missing pixels with the neighboring ones

TODO:
 - Build RGB images from 3 channels
 - composite of equalized images
 - Add support for different color palettes
 - Get the satellites name
 - set composites to be 5x4 instead of 10x2 to make them more fitting to computer screens
