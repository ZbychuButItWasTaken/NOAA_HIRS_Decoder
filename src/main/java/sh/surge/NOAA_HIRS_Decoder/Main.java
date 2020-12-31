/*
Copyright 2020 Zbigniew Sztanga

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>
*/

package sh.surge.NOAA_HIRS_Decoder;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.ini4j.Wini;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class Main {
    private static final int[] COLUMNS = {17, 18, 23, 24, 27, 28, 31, 32, 35, 36, 39, 40, 43, 44, 55, 56, 59, 60, 63, 64, 67, 68, 71, 72, 75, 76, 79, 80, 83, 84, 85, 86, 89, 90, 93, 94};
    private static final ArrayList<String> frames = new ArrayList<>();
    private static final ArrayList<Integer> frmCnt = new ArrayList<>();
    private static final ArrayList<String> framePixels = new ArrayList<>();
    private static final ArrayList<Character> validDataBit = new ArrayList<>();
    private static final ArrayList<Integer> starts = new ArrayList<>();
    private static final ArrayList<Integer> mirrorPos = new ArrayList<>();
    private static final XSSFWorkbook workbook = new XSSFWorkbook();
    private static final ArrayList<Integer> majorFrmCnt = new ArrayList<>();
    private static final ArrayList<int[]> missingLinesList = new ArrayList<>();
    //todo check for missing 7 majorframes
    //private static final ArrayList<String[]> timeList = new ArrayList<>();
    private static final ArrayList<Integer> ids = new ArrayList<>();
    private static String input, saveType;
    private static boolean saveCompo, doHis, saveXlsx, saveMsa, saveTime, cloudHe, landHe, outName, compoName, hisName, xlsxName, msaName, timeName, saveHisCombo, hisCompoName, averagePixels;
    private static boolean saveRgb, rgbRedHe, rgbBlueHe, rgbGreenHe, rgbName, saveName, fullBW, doCrop;
    private static String rgbPath1, rgbPath2;
    private static int rgbChR, rgbChG, rgbChB;
    private static String compo1, compo2, hisCompo1, hisCompo2;
    private static String out1, out2;
    private static String hisPath1, hisPath2;
    private static String xlsxP1, xlsxP2;
    private static String msaPath1, msaPath2;
    private static String timePath1, timePath2, timeFormat, timeLanguage;
    private static int landChannel, landThreshold, cloudsChannel, cloudThreshold, saveQuality, totalMissingLines;
    private static Color waterBase, landBase, cloudBase;
    private static float landBri, waterBri, cloudBri;
    private static int compoSizeW, compoSizeH, hisCompoSizeW, hisCompoSizeH;
    private static int badLineThreshold;
    private static int averagingThreshold;
    private static int firstGoodLine, croppedImgH;
    private static String name;
    private static boolean silentMode;


    @Parameter(names = {"-i", "--input"}, description = "Input File or Directory")
    private static String inputParam;

    @Parameter(names = {"-o", "--output"}, description = "Output directory")
    private static String outputParam;

    @Parameter(names = {"-c", "--config"}, description = "Configuration file")
    private static String configParam;

    @Parameter(names = {"-s", "--save_compo"}, description = "Save composition of all channels", arity = 1)
    private static boolean saveCompoParam;

    @Parameter(names = {"-a", "--average_pixels"}, description = "Average neighboring pixels", arity = 1)
    private static boolean averagePixelsParam;

    @Parameter(names = {"-e", "--silent_mode"}, description = "Disables console output", arity = 1)
    private static boolean silentModeParam;

    @Parameter(names = {"-h", "--help"}, description = "Display this list", help = true)
    private boolean help = false;

    public static void main(String[] argv) {
        Main main = new Main();
        //JCommander.newBuilder().addObject(main).build().parse(argv);
        JCommander jCommander = JCommander.newBuilder().addObject(main).build();
        jCommander.parse(argv);
        //System.out.println(outputParam + ", " + inputParam + ", " + configParam + ", " + saveCompoParam + ", " + averagePixelsParam +", " + silentModeParam);

        if (main.help){
            jCommander.usage();
            return;
        }

        List<String> stringList = new ArrayList<>(Arrays.asList(argv));

        if (stringList.contains("-s")||stringList.contains("--save_compo")){
            saveCompo = saveCompoParam;
        }
        if (stringList.contains("-a")||stringList.contains("--average_pixels")){
            averagePixels = averagePixelsParam;
        }
        if (stringList.contains("-e")||stringList.contains("--silent_mode")){
            silentMode = silentModeParam;
        }

        //System.out.println(Arrays.toString(args));
        try {
            try {
                if(configParam != null){
                    readIni(configParam);
                }else{
                    readIni("config.ini");
                }
            } catch (NullPointerException e) {
                System.out.println("Program encountered an exception wile reading the configuration! Aborting in 5s");
                e.printStackTrace();
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
                Runtime.getRuntime().exit(1);
            }
            if (!silentMode) System.out.println("Loading file....");
            File inputFile = null;

            if(inputParam != null){
                if (inputParam.endsWith(".txt")){
                    inputFile = new File(inputParam);
                }else if (new File(inputParam).isDirectory()){
                    inputFile = getLastModified(inputParam, ".txt");
                }
            }else {
                inputFile = getLastModified(input, ".txt");
            }

            if (inputFile == null) {
                if(!silentMode) System.out.println("No input file found. Aborting");
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Runtime.getRuntime().exit(1);
            }
            if (!silentMode) System.out.println("Newest file found: " + inputFile.getName());
            if (!silentMode) System.out.println("OK!" + '\n');

            if (outputParam != null){
                out1 = outputParam;
                if (outputParam.contains("[name]")) {
                    int name = outputParam.indexOf("[name]");
                    out1 = outputParam.substring(0, name);
                    out2 = outputParam.substring(name + 6);
                    outName = true;
                }
            }

            decode(inputFile);
            makeImages();

        } catch (Exception e) {
            System.out.println("Program encountered an unhandled Exception:");
            e.printStackTrace();
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            Runtime.getRuntime().exit(1);
        }

    }

    private static BufferedImage crop(BufferedImage inImage, boolean first, int CfirstGood, int Cheight) {
        int firstGood = 0;
        int firstGoodBottom = 0;
        int height;
        if (first) {
            for (int line = 0; line < inImage.getHeight() - 1; line++) {
                int goodPxs = 0;
                for (int x = 0; x < 56; x++) {
                    int av = getAverage(x, line, inImage);
                    if (Math.abs(av - new Color(inImage.getRGB(x, line)).getRed()) < averagingThreshold && av != 0) {
                        goodPxs++;
                    }
                }
                if (goodPxs > 56 * badLineThreshold / 100) {
                    firstGood = line;
                    break;
                }
            }

            for (int line = inImage.getHeight() - 1; line > 0; line--) {
                int goodPxs = 0;
                for (int x = 0; x < 56; x++) {
                    int av = getAverage(x, line, inImage);
                    if (Math.abs(av - new Color(inImage.getRGB(x, line)).getRed()) < averagingThreshold) {
                        goodPxs++;
                    }
                }
                if (goodPxs > 56 * badLineThreshold / 100) {
                    firstGoodBottom = line;
                    break;
                }
            }
            height = firstGoodBottom - firstGood;
            croppedImgH = height;
            firstGoodLine = firstGood;
        } else {
            height = Cheight;
            firstGood = CfirstGood;
        }
        if (!silentMode) System.out.println(height);

        return inImage.getSubimage(0, firstGood, 56, height);
    }

    private static File getLastModified(String path, String extension) {
        File directory = new File(path);
        File[] files = directory.listFiles(File::isFile);
        long lastModifiedTime = Long.MIN_VALUE;
        File chosenFile = null;

        if (files != null) {
            for (File file : files) {
                if (file.lastModified() > lastModifiedTime && file.getName().endsWith(extension)) {
                    chosenFile = file;
                    lastModifiedTime = file.lastModified();
                }
            }
        }

        return chosenFile;
    }

    private static BufferedImage equalize(BufferedImage src) {
        BufferedImage nImg = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster wr = src.getRaster();
        WritableRaster er = nImg.getRaster();
        int totpix = wr.getWidth() * wr.getHeight();
        int[] histogram = new int[256];

        for (int x = 0; x < wr.getWidth(); x++) {
            for (int y = 0; y < wr.getHeight(); y++) {
                histogram[wr.getSample(x, y, 0)]++;
            }
        }

        int[] chistogram = new int[256];
        chistogram[0] = histogram[0];
        for (int i = 1; i < 256; i++) {
            chistogram[i] = chistogram[i - 1] + histogram[i];
        }

        float[] arr = new float[256];
        for (int i = 0; i < 256; i++) {
            arr[i] = (float) ((chistogram[i] * 255.0) / (float) totpix);
        }

        for (int x = 0; x < wr.getWidth(); x++) {
            for (int y = 0; y < wr.getHeight(); y++) {
                int nVal = (int) arr[wr.getSample(x, y, 0)];
                er.setSample(x, y, 0, nVal);
            }
        }
        nImg.setData(er);
        return nImg;
    }

    private static void saveImg(String path, String type, BufferedImage image) {
        File outputfile = new File(path);
        if (outputfile.mkdirs() && !silentMode) System.out.println("Directory created!");
        if (saveType.equals("jpg")) {
            JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
            jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            float quality = saveQuality / 100f;
            jpegParams.setCompressionQuality(quality);
            final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            try {
                writer.setOutput(new FileImageOutputStream(outputfile));
                writer.write(null, new IIOImage(image, null, null), jpegParams);
                if (!silentMode) System.out.println("Saved " + path);
            } catch (IOException e) {
                if (!silentMode) System.out.println("Failed to save!" + path);
            }
        } else {
            try {
                ImageIO.write(image, type, outputfile);
                if (!silentMode) System.out.println("Saved " + path);
            } catch (IOException e) {
                if (!silentMode) System.out.println("Failed to save!" + path);
            }
        }
    }

    private static void generateMsa(BufferedImage cloud, BufferedImage land, String msaPath1, String msaPath2, String name, int landThreshold, int cloudThreshold) {
        BufferedImage msaImg = new BufferedImage(cloud.getWidth(), cloud.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int line = 0; line < msaImg.getHeight(); line++) {
            for (int x = 0; x < 56; x++) {
                Raster rasterLand = land.getRaster();
                Raster rasterCloud = cloud.getRaster();
                int[] pixelDataL = {0, 0, 0};
                int[] pixelDataC = {0, 0, 0};
                int[] pixelDataLand = rasterLand.getPixel(x, line, pixelDataL);
                int[] pixelDataCloud = rasterCloud.getPixel(x, line, pixelDataC);
                //msaImg.setRGB(x,line,new Color(pixelDataLand[0],pixelDataLand[0],pixelDataLand[0]).getRGB());

                try {
                    msaImg.setRGB(x, line, new Color((int) ((waterBase.getRed() * pixelDataLand[0] / 255f) * waterBri), (int) ((waterBase.getGreen() * pixelDataLand[0] / 255f) * waterBri), (int) ((waterBase.getBlue() * pixelDataLand[0] / 255f) * waterBri)).getRGB());
                } catch (IllegalArgumentException e) {
                    msaImg.setRGB(x, line, new Color((int) (waterBase.getRed() * pixelDataLand[0] / 255f), (int) (waterBase.getGreen() * pixelDataLand[0] / 255f), (int) (waterBase.getBlue() * pixelDataLand[0] / 255f)).getRGB());
                }

                //msaImg.setRGB(x, line, new Color(0, 50, 200).getRGB());
                if (pixelDataLand[0] > landThreshold) {
                    try {
                        msaImg.setRGB(x, line, new Color((int) ((landBase.getRed() * pixelDataLand[0] / 255f) * landBri), (int) ((landBase.getGreen() * pixelDataLand[0] / 255f) * landBri), (int) ((landBase.getBlue() * pixelDataLand[0] / 255f) * landBri)).getRGB());
                    } catch (IllegalArgumentException e) {
                        msaImg.setRGB(x, line, new Color((int) (landBase.getRed() * pixelDataLand[0] / 255f), (int) (landBase.getGreen() * pixelDataLand[0] / 255f), (int) (landBase.getBlue() * pixelDataLand[0] / 255f)).getRGB());
                    }
                }
                if (pixelDataCloud[0] > cloudThreshold) {
                    try {
                        msaImg.setRGB(x, line, new Color((int) ((cloudBase.getRed() * pixelDataLand[0] / 255f) * cloudBri), (int) ((cloudBase.getGreen() * pixelDataLand[0] / 255f) * cloudBri), (int) ((cloudBase.getBlue() * pixelDataLand[0] / 255f) * cloudBri)).getRGB());
                    } catch (IllegalArgumentException e) {
                        msaImg.setRGB(x, line, new Color((int) (cloudBase.getRed() * pixelDataLand[0] / 255f), (int) (cloudBase.getGreen() * pixelDataLand[0] / 255f), (int) (cloudBase.getBlue() * pixelDataLand[0] / 255f)).getRGB());

                    }
                }
            }
        }

        saveImg(msaPath1 + name + msaPath2 + "msa." + saveType, saveType, msaImg);
    }

    private static void readIni(String fileName) {
        Wini ini = null;

        try {
            ini = new Wini(new File(fileName));
        } catch (IOException e) {
            if (!silentMode) System.out.println("Configuration file not found! Aborting!");
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            Runtime.getRuntime().exit(0);
        }


        input = ini.get("main", "input");
        input = input.trim();
        if (input.charAt(input.length() - 1) != '/') {
            input = input + '/';
        }

        String output = ini.get("main", "output");
        output = output.trim();
        if (output.charAt(output.length() - 1) != '/') {
            output = output + '/';
        }


        String com = ini.get("main", "save_compo");
        saveCompo = com.equals("yes");

        String compo = ini.get("main", "compo_path");
        compo = compo.trim();
        if (compo.charAt(compo.length() - 1) != '/') {
            compo = compo + '/';
        }

        compo1 = compo;
        if (compo.contains("[name]")) {
            int name = compo.indexOf("[name]");
            compo1 = output.substring(0, name);
            compo2 = output.substring(name + 6);
            compoName = true;
        }

        if (ini.get("main", "B/W_range").equals("full")) fullBW = true;
        if (ini.get("main", "crop").equals("yes")) doCrop = true;

        saveType = ini.get("main", "save_type");
        saveQuality = Integer.parseInt(ini.get("main", "save_quality"));

        String sXlsx = ini.get("xlsx", "save_xlsx");

        if (sXlsx.equals("yes")) saveXlsx = true;

        String xlsxP = ini.get("xlsx", "xlsx_path");
        xlsxP = xlsxP.trim();
        if (xlsxP.charAt(xlsxP.length() - 1) != '/') {
            xlsxP = xlsxP + '/';
        }

        xlsxP1 = xlsxP;
        if (xlsxP.contains("[name]")) {
            int name = xlsxP.indexOf("[name]");
            xlsxP1 = output.substring(0, name);
            xlsxP2 = output.substring(name + 6);
            xlsxName = true;
        }

        String dohis = ini.get("histogram_equalization", "equalize_histogram");

        if (dohis.equals("yes")) doHis = true;
        String hisPath;

        if (doHis) {
            hisPath = ini.get("histogram_equalization", "histogram_equalization_path");
            hisPath = hisPath.trim();
            if (hisPath.charAt(hisPath.length() - 1) != '/') {
                hisPath = hisPath + '/';
            }
            hisPath1 = hisPath;
            if (hisPath.contains("[name]")) {
                int name = hisPath.indexOf("[name]");
                hisPath1 = hisPath.substring(0, name);
                hisPath2 = hisPath.substring(name + 6);
                hisName = true;
            }
        }

        out1 = output;
        if (output.contains("[name]")) {
            int name = output.indexOf("[name]");
            out1 = output.substring(0, name);
            out2 = output.substring(name + 6);
            outName = true;
        }

        String msa = ini.get("msa", "save_msa");

        if (msa.equals("yes")) saveMsa = true;
        String msaPath = ini.get("msa", "msa_path");
        msaPath = msaPath.trim();
        if (msaPath.charAt(msaPath.length() - 1) != '/') {
            msaPath = msaPath + '/';
        }

        msaPath1 = msaPath;
        if (msaPath.contains("[name]")) {
            int name = msaPath.indexOf("[name]");
            msaPath1 = msaPath.substring(0, name);
            msaPath2 = msaPath.substring(name + 6);
            msaName = true;
        }


        cloudsChannel = Integer.parseInt(ini.get("msa", "cloud_channel"));

        landChannel = Integer.parseInt(ini.get("msa", "land_channel"));
        String clouds = ini.get("msa", "cloud_channel_he"), lands = ini.get("msa", "land_channel_he");

        if (clouds.equals("yes")) cloudHe = true;
        if (lands.equals("yes")) landHe = true;

        cloudThreshold = Integer.parseInt(ini.get("msa", "cloud_threshold"));

        landThreshold = Integer.parseInt(ini.get("msa", "land_threshold"));

        String wColor = ini.get("msa", "water_base_color");
        waterBase = new Color(Integer.parseInt(wColor.substring(0, 2), 16), Integer.parseInt(wColor.substring(2, 4), 16), Integer.parseInt(wColor.substring(4, 6), 16));

        String lColor = ini.get("msa", "land_base_color");
        landBase = new Color(Integer.parseInt(lColor.substring(0, 2), 16), Integer.parseInt(lColor.substring(2, 4), 16), Integer.parseInt(lColor.substring(4, 6), 16));

        String cColor = ini.get("msa", "cloud_base_color");
        cloudBase = new Color(Integer.parseInt(cColor.substring(0, 2), 16), Integer.parseInt(cColor.substring(2, 4), 16), Integer.parseInt(cColor.substring(4, 6), 16));

        landBri = ini.get("msa", "land_brightening", float.class);
        waterBri = ini.get("msa", "water_brightening", float.class);
        cloudBri = ini.get("msa", "cloud_brightening", float.class);

        String timeS = ini.get("stat", "save_time");

        if (timeS.equals("yes")) saveTime = true;

        String nameS = ini.get("stat", "save_name");

        if (nameS.equals("yes")) saveName = true;
        String timePath = ini.get("stat", "stat_path");

        timePath = timePath.trim();
        if (timePath.charAt(timePath.length() - 1) != '/') {
            timePath = timePath + '/';
        }

        timePath1 = timePath;
        if (timePath.contains("[name]")) {
            int name = timePath.indexOf("[name]");
            timePath1 = timePath.substring(0, name);
            timePath2 = timePath.substring(name + 6);
            timeName = true;
        }
        timeFormat = ini.get("stat", "time_format");
        timeLanguage = ini.get("stat", "time_language");

        String composize = ini.get("main", "compo_size");
        String[] composizes = composize.split("x");
        compoSizeW = Integer.parseInt(composizes[0]);
        compoSizeH = Integer.parseInt(composizes[1]);
        if (compoSizeW * compoSizeH != 20 && !silentMode)
            System.out.println("Warning: Invalid composition size! This may result in unexpected program behavior!" + compoSizeW * compoSizeH);

        String savehiscompo = ini.get("histogram_equalization", "save_histogram_compo");
        if (savehiscompo.equals("yes")) saveHisCombo = true;

        String hiscompopath = ini.get("histogram_equalization", "histogram_compo_path");
        hiscompopath = hiscompopath.trim();
        if (hiscompopath.charAt(hiscompopath.length() - 1) != '/') {
            hiscompopath = hiscompopath + '/';
        }
        hisCompo1 = hiscompopath;
        if (hiscompopath.contains("[name]")) {
            int name = hiscompopath.indexOf("[name]");
            hisCompo1 = hiscompopath.substring(0, name);
            hisCompo2 = hiscompopath.substring(name + 6);
            hisCompoName = true;
        }
        String hiscomposize = ini.get("histogram_equalization", "histogram_compo_size");
        String[] hiscomposizes = hiscomposize.split("x");
        hisCompoSizeW = Integer.parseInt(hiscomposizes[0]);
        hisCompoSizeH = Integer.parseInt(hiscomposizes[1]);
        if (hisCompoSizeW * hisCompoSizeH != 20 && !silentMode)
            System.out.println("Warning: Invalid histogram equalized composition size! This may result in unexpected program behavior!" + hisCompoSizeW * hisCompoSizeH);

        String averagepixels = ini.get("main", "average_pixels");
        if (averagepixels.equals("yes")) averagePixels = true;

        String savergb = ini.get("rgb", "save_rgb");
        saveRgb = savergb.equals("yes");

        String rgbpath = ini.get("rgb", "rgb_path");
        rgbPath1 = rgbpath;
        if (rgbpath.contains("[name]")) {
            int name = rgbpath.indexOf("[name]");
            rgbPath1 = rgbpath.substring(0, name);
            rgbPath2 = rgbpath.substring(name + 6);
            rgbName = true;
        }
        rgbChR = Integer.parseInt(ini.get("rgb", "rgb_red"));
        rgbChG = Integer.parseInt(ini.get("rgb", "rgb_green"));
        rgbChB = Integer.parseInt(ini.get("rgb", "rgb_blue"));
        rgbRedHe = ini.get("rgb", "rgb_red_he").equals("yes");
        rgbGreenHe = ini.get("rgb", "rgb_green_he").equals("yes");
        rgbBlueHe = ini.get("rgb", "rgb_blue_he").equals("yes");

        badLineThreshold = Integer.parseInt(ini.get("main", "crop_threshold"));
        averagingThreshold = Integer.parseInt(ini.get("main", "averaging_threshold"));

        silentMode = ini.get("main", "silent_mode").equals("yes");
    }

    private static int getNum(int i) {
        int num = 0;
        switch (i) {
            case 0:
                num = 1;
                break;
            case 1:
                num = 17;
                break;
            case 2:
                num = 2;
                break;
            case 3:
                num = 3;
                break;
            case 4:
                num = 13;
                break;
            case 5:
                num = 4;
                break;
            case 6:
                num = 18;
                break;
            case 7:
                num = 11;
                break;
            case 8:
                num = 19;
                break;
            case 9:
                num = 7;
                break;
            case 10:
                num = 8;
                break;
            case 11:
                num = 20;
                break;
            case 12:
                num = 10;
                break;
            case 13:
                num = 14;
                break;
            case 14:
                num = 6;
                break;
            case 15:
                num = 5;
                break;
            case 16:
                num = 15;
                break;
            case 17:
                num = 12;
                break;
            case 18:
                num = 16;
                break;
            case 19:
                num = 9;
                break;
        }
        return num;
    }

    private static String getWav(int i) {
        String wav = "";
        switch (i) {
            case 0:
                wav = "14.98μm - 14.91μm";
                break;
            case 1:
                wav = "4.15μm - 4.10μm";
                break;
            case 2:
                wav = "14.81μm - 14.59μm";
                break;
            case 3:
                wav = "14.61μm - 14.36μm";
                break;
            case 4:
                wav = "4.59μm - 4.54μm";
                break;
            case 5:
                wav = "14.38μm - 14.06μm";
                break;
            case 6:
                wav = "4.02μm - 3.97μm";
                break;
            case 7:
                wav = "7.44μm - 7.22μm";
                break;
            case 8:
                wav = "3.83μm - 3.69μm";
                break;
            case 9:
                wav = "13.49μm - 13.21μm";
                break;
            case 10:
                wav = "11.39μm - 10.81μm";
                break;
            case 11:
                wav = "0.71μm - 0.66μm";
                break;
            case 12:
                wav = "12.81μm - 12.30μm";
                break;
            case 13:
                wav = "4.54μm - 4.50μm";
                break;
            case 14:
                wav = "13.79μm - 13.49μm";
                break;
            case 15:
                wav = "14.12μm - 13.81μm";
                break;
            case 16:
                wav = "4.49μm - 4.44μm";
                break;
            case 17:
                wav = "6.63μm - 6.40μm";
                break;
            case 18:
                wav = "4.47μm - 4.42μm";
                break;
            case 19:
                wav = "9.81μm - 9.60μm";
                break;
        }
        return wav;
    }

    private static String getTime(String binary) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("Etc/GMT+12"));

        int day = Integer.parseInt(binary.substring(0, 9), 2);
        long mili = Integer.parseInt(binary.substring(13, 40), 2);

        calendar.set(Calendar.DAY_OF_YEAR, day);

        SimpleDateFormat dateFormat = new SimpleDateFormat(timeFormat, Locale.forLanguageTag(timeLanguage));
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        int hours = (int) (mili / 3600000) - 24;
        calendar.set(Calendar.HOUR, hours);
        int minutes = (int) ((mili - (hours + 24) * 3600000) / 60000);
        calendar.set(Calendar.MINUTE, minutes);
        int seconds = (int) ((mili - minutes * 60000 - (hours + 24) * 3600000) / 1000);
        calendar.set(Calendar.SECOND, seconds);
        int millis = (int) (mili - seconds * 1000 - minutes * 60000 - (hours + 24) * 3600000);
        calendar.set(Calendar.MILLISECOND, millis);


        return dateFormat.format(calendar.getTime());
    }

    private static int getAverage(int x, int line, BufferedImage image) {
        final int[] zero = {0, 0, 0};
        int[] up;
        int[] down;
        int[] right;
        int[] left;
        int avVal;
        Raster imgRaster = image.getRaster();
        ArrayList<int[]> pxToAv = new ArrayList<>();

        if (line > 0) {
            up = imgRaster.getPixel(x, line - 1, new int[]{0, 0, 0});
            if (!Arrays.toString(up).equals(Arrays.toString(zero))) {
                pxToAv.add(up);
            }
        }
        if (line < image.getHeight() - 1) {
            down = imgRaster.getPixel(x, line + 1, new int[]{0, 0, 0});

            int[] leftr;
            int[] rightr;
            int[] downr;
            ArrayList<int[]> pxToAvR = new ArrayList<>();
            int avValR = 0;

            if (line + 2 < image.getHeight() - 1) {
                downr = imgRaster.getPixel(x, line + 2, new int[]{0, 0, 0});

                if (!Arrays.toString(downr).equals(Arrays.toString(zero))) {
                    pxToAvR.add(downr);
                }

            }
            if (x + 1 < 55) {
                rightr = imgRaster.getPixel(x + 1, line + 1, new int[]{0, 0, 0});
                if (!Arrays.toString(rightr).equals(Arrays.toString(zero))) {
                    pxToAvR.add(rightr);
                }

            }
            if (x - 1 > 0) {
                leftr = imgRaster.getPixel(x - 1, line + 1, new int[]{0, 0, 0});
                if (!Arrays.toString(leftr).equals(Arrays.toString(zero))) {
                    pxToAvR.add(leftr);
                }

            }
            int sum = 0;
            for (int[] in : pxToAvR) {
                sum += in[0];
            }

            if (pxToAvR.size() > 0) avValR = sum / pxToAvR.size();

            if (!Arrays.toString(down).equals(Arrays.toString(zero)) && Math.abs(imgRaster.getPixel(x, line + 1, new int[]{0, 0, 0})[0] - avValR) < 30) {
                pxToAv.add(down);
            }

        }
        if (x > 0) {
            left = imgRaster.getPixel(x - 1, line, new int[]{0, 0, 0});
            if (!Arrays.toString(left).equals(Arrays.toString(zero))) {
                pxToAv.add(left);
            }
        }
        if (x < 55) {
            right = imgRaster.getPixel(x + 1, line, new int[]{0, 0, 0});

            int[] upr;
            int[] rightr;
            int[] downr;
            ArrayList<int[]> pxToAvR = new ArrayList<>();
            int avValR = 0;

            if (line - 1 > 0) {
                upr = imgRaster.getPixel(x + 1, line - 1, new int[]{0, 0, 0});
                if (!Arrays.toString(upr).equals(Arrays.toString(zero))) {
                    pxToAvR.add(upr);
                }
            }
            if (line + 1 < image.getHeight() - 1) {
                downr = imgRaster.getPixel(x + 1, line + 1, new int[]{0, 0, 0});

                if (!Arrays.toString(downr).equals(Arrays.toString(zero))) {
                    pxToAvR.add(downr);
                }

            }
            if (x + 2 < 55) {
                rightr = imgRaster.getPixel(x + 2, line, new int[]{0, 0, 0});
                if (!Arrays.toString(rightr).equals(Arrays.toString(zero))) {
                    pxToAvR.add(rightr);
                }

            }
            int sum = 0;
            for (int[] in : pxToAvR) {
                sum += in[0];
            }

            if (pxToAvR.size() > 0) avValR = sum / pxToAvR.size();


            if (!Arrays.toString(right).equals(Arrays.toString(zero)) && Math.abs(imgRaster.getPixel(x + 1, line, new int[]{0, 0, 0})[0] - avValR) < 30) {
                pxToAv.add(right);
            }

        }
        int sum = 0;
        for (int[] in : pxToAv) {
            sum += in[0];
        }

        if (pxToAv.size() > 1) {
            avVal = sum / pxToAv.size();
        } else avVal = imgRaster.getPixel(x, line, new int[]{0, 0, 0})[0];

        return avVal;
    }

    private static String getSpacecraftName() {
        String name = "";
        ArrayList<Integer> idCount = new ArrayList<>();
        ArrayList<Integer> idNames = new ArrayList<>();
        for (int id : ids) {
            if (idCount.size() > 0) {
                boolean ins = false;
                for (int idc = 0; idc < idCount.size(); idc++) {
                    if (id == idNames.get(idc)) {
                        idCount.set(idc, idCount.get(idc) + 1);
                        ins = true;
                    }
                }
                if (!ins) {
                    idNames.add(id);
                    idCount.add(1);
                }
            } else {
                idNames.add(id);
                idCount.add(1);
            }
        }

        int LARGEST = 0;
        int lastBiggest = 0;
        for (int i = 0; i < idCount.size(); i++) {
            if (idCount.get(i) > lastBiggest) {
                LARGEST = i;
                lastBiggest = idCount.get(i);
            }
        }
        int ID = idNames.get(LARGEST);

        switch (ID) {
            case 15:
                name = "NOAA 19";
                break;
            case 13:
                name = "NOAA 18";
                break;
            case 8:
                name = "NOAA 15";
                break;
        }

        return name;
    }

    public static void decode(File inputFile) throws IOException {
        if (!silentMode) System.out.println("Converting data....");
        name = inputFile.getName().substring(0, inputFile.getName().length() - 4);

        XSSFSheet sheet = workbook.createSheet("Data");
        int rownum = 0;

        Scanner sc = new Scanner(inputFile);
        FileInputStream fis = new FileInputStream(inputFile);
        byte[] byteArray = new byte[(int) inputFile.length()];
        fis.read(byteArray);
        String datacnt = new String(byteArray);
        String[] stringArray = datacnt.split("\n");
        int numOfLines = stringArray.length;
        int j = 0;

        while (sc.hasNextLine()) {
            ArrayList<String> line = new ArrayList<>(Arrays.asList(sc.nextLine().split(" ")));
            Row row = sheet.createRow(rownum++);
            for (int i = 0; i < line.size(); i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(line.get(i));
            }
            int prc = 100 * j / numOfLines + 1;
            if (!silentMode) System.out.print('\r');
            if (!silentMode) System.out.print(generateFancyProgressBar(prc));
            j++;
        }
        if (!silentMode) System.out.print('\n');


        ArrayList<String> hexFrames = new ArrayList<>();

        if (!silentMode) System.out.println("OK!" + '\n');

        if (!silentMode) System.out.println("Reading data....");
        // For each Row.
        int rowNum = 0;
        File tmp = new File("Time.tmp");
        PrintWriter out = new PrintWriter(tmp);
        if (saveTime) {
            File timefile;
            if (timeName) {
                timefile = new File(timePath1 + name + timePath2);
            } else {
                timefile = new File(timePath1);
            }
            if (timefile.mkdirs() && !silentMode) System.out.println("Directory created!");
            if (timeName) {
                timefile = new File(timePath1 + name + timePath2 + "stat.txt");
            } else {
                timefile = new File(timePath1 + "stat.txt");
            }
            out = new PrintWriter(timefile);
        }

        int cnt = 0;
        for (Row row : sheet) {
            Cell cell = row.getCell(5);
            char fb = '0';

            switch (cell.getCellType()) {
                case NUMERIC:
                    fb = Integer.toBinaryString(Integer.parseInt("" + (int) cell.getNumericCellValue(), 16)).charAt(Integer.toBinaryString(Integer.parseInt("" + (int) cell.getNumericCellValue(), 16)).length() - 1);
                    break;
                case STRING:
                    fb = Integer.toBinaryString(Integer.parseInt(cell.getStringCellValue(), 16)).charAt(Integer.toBinaryString(Integer.parseInt(cell.getStringCellValue(), 16)).length() - 1);
                    break;
            }

            cell = row.getCell(6);

            String value;

            switch (cell.getCellType()) {
                case NUMERIC:
                    value = Integer.toBinaryString(Integer.parseInt("" + (int) cell.getNumericCellValue(), 16));

                    StringBuilder addBuilder = new StringBuilder();
                    for (int o = 0; o < 8 - value.length(); o++) {
                        addBuilder.append("0");
                    }
                    String add = addBuilder.toString();
                    value = add + value;
                    value = fb + value;
                    frmCnt.add(Integer.parseInt(value, 2));
                    break;
                case STRING:
                    value = Integer.toBinaryString(Integer.parseInt(cell.getStringCellValue(), 16));
                    add = "";
                    StringBuilder addBuilder1 = new StringBuilder(add);
                    for (int o = 0; o < 8 - value.length(); o++) {
                        addBuilder1.append("0");
                    }
                    add = addBuilder1.toString();
                    value = add + value;
                    value = fb + value;
                    frmCnt.add(Integer.parseInt(value, 2));
                    break;
            }
            //System.out.println(frmCnt.get(frmCnt.size()-1));


            cell = row.getCell(17);

            switch (cell.getCellType()) {
                case NUMERIC:
                    mirrorPos.add(Integer.parseInt("" + (int) cell.getNumericCellValue(), 16));
                    break;
                case STRING:
                    mirrorPos.add(Integer.parseInt(cell.getStringCellValue(), 16));
                    break;
            }

            cell = row.getCell(4);
            String binMFC = "";

            switch (cell.getCellType()) {
                case NUMERIC:
                    binMFC = Integer.toBinaryString(Integer.parseInt(String.valueOf((int) cell.getNumericCellValue()), 16));
                    break;
                case STRING:
                    binMFC = Integer.toBinaryString(Integer.parseInt(cell.getStringCellValue(), 16));
                    break;
            }


            StringBuilder zer = new StringBuilder();

            for (int o = 0; o < 8 - binMFC.length(); o++) {
                zer.append("0");
            }
            binMFC = zer.toString() + binMFC;

            cell = row.getCell(3);
            String nam = "";

            switch (cell.getCellType()) {
                case NUMERIC:
                    nam = Integer.toBinaryString(Integer.parseInt(String.valueOf((int) cell.getNumericCellValue()), 16));
                    break;
                case STRING:
                    nam = Integer.toBinaryString(Integer.parseInt(cell.getStringCellValue(), 16));
                    break;
            }


            StringBuilder namb = new StringBuilder();

            for (int o = 0; o < 8 - nam.length(); o++) {
                namb.append("0");
            }
            nam = namb.toString() + nam;

            nam = nam.substring(4);
            int id = Integer.parseInt(nam, 2);
            ids.add(id);

            majorFrmCnt.add(Integer.parseInt(binMFC.substring(3, 6), 2));

            if (frmCnt.get(frmCnt.size() - 1) == 0) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 9; i < 14; i++) {
                    cell = row.getCell(i);

                    switch (cell.getCellType()) {
                        case NUMERIC:
                            String str = Integer.toBinaryString(Integer.parseInt(String.valueOf((int) cell.getNumericCellValue()), 16));
                            StringBuilder add = new StringBuilder();
                            for (int o = 0; o < 8 - str.length(); o++) {
                                add.append("0");
                            }
                            str = add.toString() + str;
                            stringBuilder.append(str);
                            break;
                        case STRING:
                            String str1 = Integer.toBinaryString(Integer.parseInt(cell.getStringCellValue(), 16));
                            StringBuilder add1 = new StringBuilder();
                            for (int o = 0; o < 8 - str1.length(); o++) {
                                add1.append("0");
                            }
                            str1 = add1.toString() + str1;
                            stringBuilder.append(str1);
                            break;
                    }
                }
                String[] time = {String.valueOf(rowNum), stringBuilder.toString()};
                //timeList.add(time);
                out.println(getTime(time[1]));
            }

            for (int column : COLUMNS) {
                cell = row.getCell(column);

                if (cell == null) {
                    hexFrames.add("00");
                } else {
                    switch (cell.getCellType()) {
                        case NUMERIC:
                            hexFrames.add(String.valueOf((int) cell.getNumericCellValue()));
                            break;
                        case STRING:
                            hexFrames.add(cell.getStringCellValue());
                            break;
                    }
                }
            }
            StringBuilder frame = new StringBuilder();
            for (String hexFrame : hexFrames) {
                int val = Integer.parseInt(hexFrame, 16);
                String bin = Integer.toBinaryString(val);
                StringBuilder add = new StringBuilder();

                for (int o = 0; o < 8 - bin.length(); o++) {
                    add.append("0");
                }
                bin = add.toString() + bin;

                frame.append(bin);
            }
            frames.add(frame.toString());
            hexFrames.clear();
            if (!silentMode) System.out.print('\r');
            if (!silentMode) System.out.print(generateFancyProgressBar(100 * cnt / numOfLines + 1));

            rowNum++;
            cnt++;
        }
        if (!silentMode) System.out.print('\n');

        String spacecraftName = getSpacecraftName();
        if (!silentMode) System.out.println(spacecraftName);
        if (saveName) out.println('\n' + spacecraftName);

        out.close();
        if (tmp.delete()) {
            if (!silentMode) System.out.println("Tmp files deleted!");
        }
        if (!silentMode) System.out.println("OK, found " + frmCnt.size() + " lines." + '\n');

        if (!silentMode) System.out.println("Checking for missing data....");
        for (int i = 0; i < frmCnt.size(); i++) {
            if (frmCnt.get(i) == 0 || frmCnt.get(i) == 64 || frmCnt.get(i) == 128 || frmCnt.get(i) == 192 || frmCnt.get(i) == 256) {
                if (frmCnt.get(i) % 64 == mirrorPos.get(i)) {
                    starts.add(i);
                }
            }
        }

        for (String frame : frames) {
            framePixels.add(frame.substring(26, 286));
            validDataBit.add(frame.charAt(286));
        }

        for (int i = 1; i < starts.size(); i++) {
            int missingLines = 0;
            if (frmCnt.get(starts.get(i)) - 64 != frmCnt.get(starts.get(i - 1)) && frmCnt.get(starts.get(i)) != 0) {
                if (!silentMode)
                    System.out.println("Found missing data at: " + i + " (" + frmCnt.get(starts.get(i - 1)) + "; " + frmCnt.get(starts.get(i)) + "; " + majorFrmCnt.get(starts.get(i - 1)) + "; " + majorFrmCnt.get(starts.get(i)) + ")");
                if (majorFrmCnt.get(starts.get(i - 1)).equals(majorFrmCnt.get(starts.get(i)))) {
                    missingLines = (frmCnt.get(starts.get(i)) - frmCnt.get(starts.get(i - 1))) / 64 - 1;
                } else if (starts.get(i - 1) < frmCnt.get(starts.get(i))) {
                    missingLines = (320 - starts.get(i - 1)) / 64 + starts.get(i) / 64 - 1;
                }
            }
            int[] data = {i, missingLines};
            missingLinesList.add(data);
            totalMissingLines += missingLines;

        }
        if (!silentMode) System.out.println();

    }

    public static void makeImages() {
        BufferedImage compoImg = new BufferedImage(56 * compoSizeW, (starts.size() + totalMissingLines) * compoSizeH, BufferedImage.TYPE_INT_RGB);
        BufferedImage hisCompoImg = new BufferedImage(56 * hisCompoSizeW, (starts.size() + totalMissingLines) * hisCompoSizeH, BufferedImage.TYPE_INT_RGB);

        Graphics g = compoImg.getGraphics();
        Graphics gh = hisCompoImg.getGraphics();

        BufferedImage cloud = new BufferedImage(56, starts.size() + totalMissingLines, BufferedImage.TYPE_INT_RGB);
        BufferedImage land = new BufferedImage(56, starts.size() + totalMissingLines, BufferedImage.TYPE_INT_RGB);
        BufferedImage rgbImage = new BufferedImage(56, starts.size() + totalMissingLines, BufferedImage.TYPE_INT_RGB);

        for (int i = 0; i < 20; i++) {
            BufferedImage image = new BufferedImage(56, starts.size() + totalMissingLines, BufferedImage.TYPE_INT_RGB);

            rgbImage.getData().createCompatibleWritableRaster();
            int skipped = 0;

            for (int line = 0; line < starts.size(); line++) {

                for (int[] data : missingLinesList) {
                    if (data[0] == line) {
                        for (int w = 0; w < data[1]; w++) {
                            skipped++;
                        }
                    }
                }

                for (int o = 0; o < 56; o++) {
                    Color color;
                    int x;
                    try {
                        x = mirrorPos.get(starts.get(line) + o - 1);

                        if (x < 56 && validDataBit.get(starts.get(line) + o - 1) == '1') {

                            boolean negative = false;
                            if (framePixels.get(starts.get(line) + o).charAt(13 * i) == '0') negative = true;
                            int val = Integer.parseInt(framePixels.get(starts.get(line) + o).substring(13 * i + 1, 13 * i + 13), 2);
                            int col;
                            if (fullBW) {
                                if (negative) {
                                    val = 4095 - val;
                                } else {
                                    val += 4095;
                                }

                                col = (255 * val) / 8190;
                            } else {
                                col = (255 * val) / 4095;
                            }


                            color = new Color(col, col, col);

                            //if (negative){
                            //    color = new Color(col, 0, 0);
                            //}

                            image.setRGB(x, line + skipped, color.getRGB());

                        }
                    } catch (IndexOutOfBoundsException ignored) {

                    }
                }

            }

            if (doCrop) {
                if (i == 0) {
                    image = crop(image, true, 0, 0);
                    compoImg = new BufferedImage(56 * compoSizeW, image.getHeight() * compoSizeH, BufferedImage.TYPE_INT_RGB);
                    hisCompoImg = new BufferedImage(56 * hisCompoSizeW, image.getHeight() * hisCompoSizeH, BufferedImage.TYPE_INT_RGB);

                    g = compoImg.getGraphics();
                    gh = hisCompoImg.getGraphics();
                    cloud = new BufferedImage(56, starts.size() + totalMissingLines, BufferedImage.TYPE_INT_RGB);
                    land = new BufferedImage(56, image.getHeight(), BufferedImage.TYPE_INT_RGB);
                    rgbImage = new BufferedImage(56, image.getHeight(), BufferedImage.TYPE_INT_RGB);
                } else {
                    image = crop(image, false, firstGoodLine, croppedImgH);

                }
            }


            if (averagePixels) {
                BufferedImage avrImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_BGR);
                Raster imgRaster = image.getRaster();
                int[] pat = {0, 0, 0};

                for (int i1 = 0; i1 < avrImg.getWidth() * avrImg.getHeight(); i1++) {
                    int px = i1 % 56;
                    int line = (i1 - px) / 56;
                    int av = getAverage(px, line, image);

                    if (Math.abs(imgRaster.getPixel(px, line, pat)[0] - av) > averagingThreshold) {
                        image.setRGB(px, line, new Color(av, av, av).getRGB());
                    }
                }
            }

            int num = getNum(i);
            String wav = getWav(i);


            if (saveCompo) {
                int x = (num - 1) % compoSizeW;
                int line = (num - 1 - x) / compoSizeW;
                g.drawImage(image, x * 56, line * image.getHeight(), null);
            }

            if (saveHisCombo) {
                int x = (num - 1) % hisCompoSizeW;
                int line = (num - 1 - x) / hisCompoSizeW;
                gh.drawImage(equalize(image), x * 56, line * image.getHeight(), null);
            }

            if (outName) {
                saveImg(out1 + name + out2 + "Channel " + (num) + " (" + wav + ")" + "." + saveType, saveType, image);
            } else {
                saveImg(out1 + "Channel " + (num) + " (" + wav + ")" + "." + saveType, saveType, image);
            }


            if (doHis) {
                if (hisName) {
                    saveImg(hisPath1 + name + hisPath2 + "Channel " + (num) + " (" + wav + ")" + "." + saveType, saveType, equalize(image));
                } else {
                    saveImg(hisPath1 + "Channel " + (num) + " (" + wav + ")" + "." + saveType, saveType, equalize(image));
                }
            }

            if (saveMsa) {
                if (num == cloudsChannel) {
                    if (!cloudHe) cloud = image;
                    if (cloudHe) cloud = equalize(image);
                }
                if (num == landChannel) {
                    if (!landHe) land = image;
                    if (landHe) land = equalize(image);
                }
            }

            if (saveRgb) {
                for (int i1 = 0; i1 < image.getWidth() * image.getHeight(); i1++) {
                    int x = i1 % 56;
                    int line = (i1 - x) / 56;
                    if (getNum(i) == rgbChR) {
                            /*
                            Raster inRast;
                            if (rgbRedHe){
                                inRast = equalize(image).getData();
                            }else {
                                inRast = image.getData();
                            }
                            int[] val = rast.getPixel(x, line, new int[] {0,0,0});
                            val[0] = inRast.getPixel(x,line, new int[]{0,0,0})[0];
                            rgbImage.setRGB(x, line, new Color(val[0], val[1], val[2]).getRGB());

                             */

                        Color exCol = new Color(rgbImage.getRGB(x, line));
                        rgbImage.setRGB(x, line, new Color(new Color(image.getRGB(x, line)).getRed(), exCol.getGreen(), exCol.getBlue()).getRGB());
                        if (rgbRedHe)
                            rgbImage.setRGB(x, line, new Color(new Color(equalize(image).getRGB(x, line)).getRed(), exCol.getGreen(), exCol.getBlue()).getRGB());
                    }
                    if (getNum(i) == rgbChG) {
                            /*
                            Raster inRast;
                            if (rgbGreenHe){
                                inRast = equalize(image).getData();
                            }else {
                                inRast = image.getData();
                            }
                            int[] val = rast.getPixel(x, line, new int[] {0,0,0});
                            val[1] = inRast.getPixel(x,line, new int[]{0,0,0})[0];
                            rgbImage.setRGB(x, line, new Color(val[0], val[1], val[2]).getRGB());

                             */
                        Color exCol = new Color(rgbImage.getRGB(x, line));
                        rgbImage.setRGB(x, line, new Color(exCol.getRed(), new Color(image.getRGB(x, line)).getGreen(), exCol.getBlue()).getRGB());
                        if (rgbGreenHe)
                            rgbImage.setRGB(x, line, new Color(exCol.getRed(), new Color(equalize(image).getRGB(x, line)).getGreen(), exCol.getBlue()).getRGB());
                    }
                    if (getNum(i) == rgbChB) {
                            /*
                            Raster inRast;
                            if (rgbBlueHe){
                                inRast = equalize(image).getData();
                            }else {
                                inRast = image.getData();
                            }
                            int[] val = rast.getPixel(x, line, new int[] {0,0,0});
                            val[2] = inRast.getPixel(x,line, new int[]{0,0,0})[0];
                            rgbImage.setRGB(x, line, new Color(val[0], val[1], val[2]).getRGB());

                             */
                        Color exCol = new Color(rgbImage.getRGB(x, line));
                        rgbImage.setRGB(x, line, new Color(exCol.getRed(), exCol.getGreen(), new Color(image.getRGB(x, line)).getBlue()).getRGB());
                        if (rgbBlueHe)
                            rgbImage.setRGB(x, line, new Color(exCol.getRed(), exCol.getGreen(), new Color(equalize(image).getRGB(x, line)).getBlue()).getRGB());
                    }
                }
            }

        }

        if (saveRgb) {
            if (rgbName) {
                saveImg(rgbPath1 + name + rgbPath2 + "Rgb." + saveType, saveType, rgbImage);
            } else {
                saveImg(rgbPath1 + "Rgb." + saveType, saveType, rgbImage);
            }
        }

        if (saveMsa) {
            if (msaName) {
                generateMsa(cloud, land, msaPath1, msaPath2, name, landThreshold, cloudThreshold);
            } else {
                generateMsa(cloud, land, msaPath1, "", "", landThreshold, cloudThreshold);
            }
        }

        if (saveCompo) {
            if (compoName) {
                saveImg(compo1 + name + compo2 + "Compo." + saveType, saveType, compoImg);
            } else {
                saveImg(compo1 + "Compo." + saveType, saveType, compoImg);
            }
        }
        if (saveHisCombo) {
            if (hisCompoName) {
                saveImg(hisCompo1 + name + hisCompo2 + "HE_Compo." + saveType, saveType, hisCompoImg);
            } else {
                saveImg(hisCompo1 + "HE_Compo." + saveType, saveType, hisCompoImg);
            }
        }

        try {
            if (saveXlsx) {
                File file;
                if (xlsxName) {
                    file = new File(xlsxP1 + name + xlsxP2);
                } else {
                    file = new File(xlsxP1);
                }
                if (file.mkdirs() && !silentMode) System.out.println("Directory created!");

                if (xlsxName) {
                    file = new File(xlsxP1 + name + xlsxP2 + name + ".xlsx");
                } else {
                    file = new File(xlsxP1 + name + ".xlsx");
                }
                FileOutputStream outs = new FileOutputStream(file);
                workbook.write(outs);
                outs.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String generateFancyProgressBar(int percent) {
        int part = percent / 5;
        StringBuilder buff = new StringBuilder();
        buff.append("[");

        for (int i = 0; i < part && part < 11; i++) {
            buff.append('=');
        }
        for (int i = 0; i < 10 - part && part < 11; i++) {
            buff.append(' ');
        }
        if (part > 10) buff.append("==========");
        buff.append(percent);
        buff.append("%");
        if (part < 10) buff.append("          ");

        for (int i = 10; i < part; i++) {
            buff.append('=');
        }
        for (int i = 0; i < 20 - part && part > 9; i++) {
            buff.append(' ');
        }
        buff.append(']');

        return buff.toString();
    }
}
