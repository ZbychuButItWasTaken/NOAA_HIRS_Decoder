# NOAA_HIRS_Decoder
Decoder for decoding HIRS data From NOAA satellites

TODO:
[HIGH PRIORITY]
[ ]Equalize bad/missing pixels with the neigboring ones

[MEDIUM PRIORITY]
[ ]Build RGB images fron 3 channels
[ ]combposite of equalized images
[ ]Add support for different color palettes

[LOW PRIORITY]
[ ]Get the sattelites name
[ ]set combposites to be 5x4 instead of 10x2 to make them more fitting to computer screens

[DONE]
[x]decode HIRS data to pictures
[x]add image equalizing
[x]add multispectral analyses
[x]multispectral analyses with brightness values
[x]add combposite of all channels
[x]option to use a common folder
[x]Do not jump missing minorframes but black them out in the image
[x]save the time of the pass
