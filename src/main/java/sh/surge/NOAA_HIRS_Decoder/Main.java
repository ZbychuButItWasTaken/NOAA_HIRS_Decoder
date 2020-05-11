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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class Main {
    private static final int[] COLUMNS = {17, 18, 23, 24, 27, 28, 31, 32, 35, 36, 39, 40, 43, 44, 55, 56, 59, 60, 63, 64, 67, 68, 71, 72, 75, 76, 79, 80, 83, 84, 85, 86, 89, 90, 93, 94};
    private static final ArrayList<String> frames = new ArrayList<>();
    private static final ArrayList<Integer> frmCnt = new ArrayList<>();
    private static final ArrayList<String> framePixels = new ArrayList<>();
    private static final ArrayList<Integer> starts = new ArrayList<>();
    private static final ArrayList<Integer> mirrorPos = new ArrayList<>();
    private static final XSSFWorkbook workbook = new XSSFWorkbook();
    private static final ArrayList<Integer> majorFrmCnt = new ArrayList<>();
    private static final ArrayList<int[]> missingLinesList = new ArrayList<>();
    private static final ArrayList<String[]> timeList = new ArrayList<>();

    private static String input, saveType;
    private static boolean saveCompo, doHis, saveXlsx, saveMsa, saveTime, cloudHe, landHe, outName, compoName, hisName, xlsxName, msaName, timeName;
    private static String compo1 = null, compo2 = null;
    private static String out1, out2;
    private static String hisPath1, hisPath2;
    private static String xlsxP1, xlsxP2;
    private static String msaPath1, msaPath2;
    private static String timePath1, timePath2, timeFormat, timeLanguage;
    private static int landChannel, landThreshold, cloudsChannel, cloudThreshold, saveQuality, totalMissingLines;
    private static Color waterBase, landBase, cloudBase;
    private static float landBri, waterBri, cloudBri;


    public static void main(String[] args) {
        readIni();

        //TODO Read file from main() argument

        String name = "";
        try {
            System.out.println("Loading file....");
            File inputFile = getLastModified(input);

            if (inputFile == null) {
                System.out.println("No input file found. Aborting");
                TimeUnit.SECONDS.sleep(5);
                Runtime.getRuntime().exit(0);
            }
            System.out.println("Newest file found: " + inputFile.getName());
            name = inputFile.getName().substring(0, inputFile.getName().length() - 4);
            System.out.println("OK!" + '\n');
            System.out.println("Converting data....");

            XSSFSheet sheet = workbook.createSheet("Data");
            int rownum = 0;

            Scanner sc = new Scanner(inputFile);

            while (sc.hasNextLine()) {
                ArrayList<String> line = new ArrayList<>(Arrays.asList(sc.nextLine().split(" ")));
                Row row = sheet.createRow(rownum++);
                for (int i = 0; i < line.size(); i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(line.get(i));
                }
            }


            ArrayList<String> hexFrames = new ArrayList<>();

            System.out.println("OK!" + '\n');

            System.out.println("Readnig data....");
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
                if (timefile.mkdirs()) System.out.println("Directory created!");
                if (timeName) {
                    timefile = new File(timePath1 + name + timePath2 + "time.txt");
                } else {
                    timefile = new File(timePath1 + "time.txt");
                }
                out = new PrintWriter(timefile);
            }
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
                    timeList.add(time);
                    out.println(getTime(time[1]));
                }

                for (int column : COLUMNS) {
                    cell = row.getCell(column);

                    switch (cell.getCellType()) {
                        case NUMERIC:
                            hexFrames.add(String.valueOf((int) cell.getNumericCellValue()));
                            break;
                        case STRING:
                            hexFrames.add(cell.getStringCellValue());
                            break;
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
                rowNum++;
            }

            out.close();
            if (tmp.delete()) {
                System.out.println("Tmp files deleted!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("OK, found " + frmCnt.size() + " lines." + '\n');

        System.out.println("Checking for missing data!");
        for (int i = 0; i < frmCnt.size(); i++) {
            if (frmCnt.get(i) == 0 || frmCnt.get(i) == 64 || frmCnt.get(i) == 128 || frmCnt.get(i) == 192 || frmCnt.get(i) == 256) {
                if (frmCnt.get(i) % 64 == mirrorPos.get(i)) {
                    starts.add(i);
                }
            }
        }

        for (String frame : frames) {
            framePixels.add(frame.substring(26, 286));
        }

        for (int i = 1; i < starts.size(); i++) {
            int missingLines = 0;
            if (frmCnt.get(starts.get(i)) - 64 != frmCnt.get(starts.get(i - 1)) && frmCnt.get(starts.get(i)) != 0) {
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
        System.out.println();


        BufferedImage compoImg = new BufferedImage(56 * 10, starts.size() * 2, BufferedImage.TYPE_INT_RGB);

        Graphics g = compoImg.getGraphics();

        /*
        File folder;
        if (normName) {
            folder = new File(out1 + name + out2);
            if (folder.mkdir()) {
                System.out.println(out1 + name + out2 + " Directory created!");
            }
        } else {
            folder = new File(out1);
            if (folder.mkdir()) {
                System.out.println(out1 + " Directory created!");
            }
        }

        if (doHis) {
            if (hisName) {
                folder = new File(hisPath1 + name + hisPath2);
                if (folder.mkdir()) {
                    System.out.println(hisPath1 + name + hisPath2 + " Directory created!");
                }
            } else {
                folder = new File(hisPath1);
                if (folder.mkdir()) {
                    System.out.println(hisPath1 + " Directory created!");
                }
            }
        }
        if (saveXlsx) {
            if (xlsxName) {
                folder = new File(xlsxP1 + name + xlsxP2);
                if (folder.mkdir()) {
                    System.out.println(xlsxP1 + name + xlsxP2 + " Directory created!");
                }
            } else {
                folder = new File(xlsxP1);
                if (folder.mkdir()) {
                    System.out.println(xlsxP1 + " Directory created!");
                }
            }
        }
        if (saveCompo) {
            if (compoName) {
                folder = new File(compo1 + name + compo2);
                if (folder.mkdir()) {
                    System.out.println(compo1 + name + compo2 + " Directory created!");
                }
            } else {
                folder = new File(compo1);
                if (folder.mkdir()) {
                    System.out.println(compo1 + " Directory created!");
                }
            }
        }


        if (saveMsa) {
            if (msaName) {
                folder = new File(msaPath1 + name + msaPath2);
                if (folder.mkdir()) {
                    System.out.println(msaPath1 + name + msaPath2 + " Directory created!");
                }
            } else {
                folder = new File(msaPath1 + msaPath2);
                if (folder.mkdir()) {
                    System.out.println(msaPath1 + " Directory created!");
                }
            }
        }
        */

        BufferedImage cloud = new BufferedImage(56, starts.size() + totalMissingLines, BufferedImage.TYPE_INT_RGB);
        BufferedImage land = new BufferedImage(56, starts.size() + totalMissingLines, BufferedImage.TYPE_INT_RGB);

        for (int i = 0; i < 20; i++) {
            BufferedImage image = new BufferedImage(56, starts.size() + totalMissingLines, BufferedImage.TYPE_INT_RGB);
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

                        if (x < 56 && x != starts.get(line + 1)) {

                            boolean negative = false;
                            if (framePixels.get(starts.get(line) + o).charAt(13 * i) == '0') negative = true;
                            int val = Integer.parseInt(framePixels.get(starts.get(line) + o).substring(13 * i + 1, 13 * i + 13), 2);
                            if (negative) {
                                val = 4095 - val;
                            } else {
                                val += 4095;
                            }

                            int col = (255 * val) / 8190;

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

            //TODO Equalize missing pixels


            BufferedImage avrImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_BGR);
            Raster imgRaster = image.getRaster();
            int[] pat = {0, 0, 0};

            for (int i1 = 0; i1 < avrImg.getWidth() * avrImg.getHeight(); i1++) {
                int px = i1 % 56;
                int line = (i1 - px) / 56;
                int av = getAverage(px, line, image, false);

                if (Math.abs(imgRaster.getPixel(px, line, pat)[0] - av) > 30) {
                    image.setRGB(px, line, new Color(av, av, av).getRGB());
                }
            }

            int num = getNum(i);
            String wav = getWav(i);

            //TODO Histogram equalized composite

            if (saveCompo) {
                if (num < 11) {
                    g.drawImage(image, (num - 1) * 56, 0, null);
                } else {
                    g.drawImage(image, (num - 11) * 56, image.getHeight(), null);
                }
            }

            //TODO change composite to 5x4

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

        }

        if (saveMsa) {
            if (msaName) {
                generateMsa(cloud, land, msaPath1, msaPath2, name, landThreshold, cloudThreshold);
            } else {
                generateMsa(cloud, land, msaPath1, "", "", landThreshold, cloudThreshold);
            }
        }

        if (saveCompo)
            if (compoName) {
                saveImg(compo1 + name + compo2 + "Compo." + saveType, saveType, compoImg);
            } else {
                saveImg(compo1 + "Compo." + saveType, saveType, compoImg);
            }

        try {
            if (saveXlsx) {
                File file;
                if (xlsxName) {
                    file = new File(xlsxP1 + name + xlsxP2);
                } else {
                    file = new File(xlsxP1);
                }
                if (file.mkdirs()) System.out.println("Directory created!");

                if (xlsxName) {
                    file = new File(xlsxP1 + name + xlsxP2 + name + ".xlsx");
                } else {
                    file = new File(xlsxP1 + name + ".xlsx");
                }
                FileOutputStream out = new FileOutputStream(file);
                workbook.write(out);
                out.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static File getLastModified(String path) {
        File directory = new File(path);
        File[] files = directory.listFiles(File::isFile);
        long lastModifiedTime = Long.MIN_VALUE;
        File chosenFile = null;

        if (files != null) {
            for (File file : files) {
                if (file.lastModified() > lastModifiedTime) {
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
        if (outputfile.mkdirs()) System.out.println("Directory created!");
        if (saveType.equals("jpg")) {
            JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
            jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            float quality = saveQuality / 100f;
            jpegParams.setCompressionQuality(quality);
            final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            try {
                writer.setOutput(new FileImageOutputStream(outputfile));
                writer.write(null, new IIOImage(image, null, null), jpegParams);
                System.out.println("Saved " + path);
            } catch (IOException e) {
                System.out.println("Failed to save!" + path);
            }
        } else {
            try {
                ImageIO.write(image, type, outputfile);
                System.out.println("Saved " + path);
            } catch (IOException e) {
                System.out.println("Failed to save!" + path);
            }
        }
    }

    private static void generateMsa(BufferedImage cloud, BufferedImage land, String msaPath1, String msaPath2, String name, int landThreshold, int cloudThreshold) {
        BufferedImage msaImg = new BufferedImage(cloud.getWidth(), land.getHeight(), BufferedImage.TYPE_INT_RGB);
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

    private static void readIni() {
        Wini ini = null;

        try {
            ini = new Wini(new File("config.ini"));
        } catch (IOException e) {
            System.out.println("Configuration file not found! Aborting!");
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            Runtime.getRuntime().exit(0);
        }


        input = ini.get("paths", "input");
        input = input.trim();
        if (input.charAt(input.length() - 1) != '/') {
            input = input + '/';
        }

        String output = ini.get("paths", "output");
        output = output.trim();
        if (output.charAt(output.length() - 1) != '/') {
            output = output + '/';
        }


        String com = ini.get("paths", "save_compo");
        saveCompo = com.equals("yes");

        String compo = ini.get("paths", "compo_path");
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

        saveType = ini.get("paths", "save_type");
        saveQuality = Integer.parseInt(ini.get("paths", "save_quality"));

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

        String timeS = ini.get("time", "save_time");

        if (timeS.equals("yes")) saveTime = true;
        String timePath = ini.get("time", "time_path");

        timePath = timePath.trim();
        if (timePath.charAt(timePath.length() - 1) != '/') {
            timePath = timePath + '/';
        }

        timePath1 = timePath;
        if (timePath.contains("[name]")) {
            int name = msaPath.indexOf("[name]");
            timePath1 = timePath.substring(0, name);
            timePath2 = timePath.substring(name + 6);
            timeName = true;
        }
        timeFormat = ini.get("time", "time_format");
        timeLanguage = ini.get("time", "time_language");
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
                wav = "12.59μm - 12.34μm";
                break;
            case 11:
                wav = "0.71μm - 0.66μm";
                break;
            case 12:
                wav = "9.82μm - 9.59μm";
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
                wav = "11.33μm - 10.89μm";
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

        int hours = (int) (mili / 3600000) - 12;
        calendar.set(Calendar.HOUR, hours);
        int minutes = (int) ((mili - (hours + 12) * 3600000) / 60000);
        calendar.set(Calendar.MINUTE, minutes);
        int seconds = (int) ((mili - minutes * 60000 - (hours + 12) * 3600000) / 1000);
        calendar.set(Calendar.SECOND, seconds);
        int millis = (int) (mili - seconds * 1000 - minutes * 60000 - (hours + 12) * 3600000);
        calendar.set(Calendar.MILLISECOND, millis);


        return dateFormat.format(calendar.getTime());
    }

    private static int getAverage(int x, int line, BufferedImage image, boolean check) {
        final int[] zero = {0, 0, 0};
        int[] up;
        int[] down;
        int[] right;
        int[] left;
        int avVal = 0;
        Raster imgRaster = image.getRaster();
        ArrayList<int[]> pxToAv = new ArrayList<>();

        if (line > 0 && !check) {
            up = imgRaster.getPixel(x, line - 1, new int[]{0, 0, 0});
            if (!Arrays.toString(up).equals(Arrays.toString(zero))) {
                pxToAv.add(up);
            }
        }
        if (line < image.getHeight() - 1) {
            down = imgRaster.getPixel(x, line + 1, new int[]{0, 0, 0});
            try {
                if (!Arrays.toString(down).equals(Arrays.toString(zero)) /*&& Math.abs(getAverage(x, line - 1, image, true) - down[0]) > 30*/) {
                    pxToAv.add(down);
                }
            }catch (ArrayIndexOutOfBoundsException e){
                if (!Arrays.toString(down).equals(Arrays.toString(zero))) {
                    pxToAv.add(down);
                }
            }
        }
        if (x > 0 && !check) {
            left = imgRaster.getPixel(x - 1, line, new int[]{0, 0, 0});
            if (!Arrays.toString(left).equals(Arrays.toString(zero))) {
                pxToAv.add(left);
            }
        }
        if (x < 55) {
            right = imgRaster.getPixel(x + 1, line, new int[]{0, 0, 0});
            try {
                if (!Arrays.toString(right).equals(Arrays.toString(zero)) /*&& Math.abs(getAverage(x + 1, line, image, true) - right[0]) > 30*/) {
                    pxToAv.add(right);
                }
            }catch (ArrayIndexOutOfBoundsException e){
                if (!Arrays.toString(right).equals(Arrays.toString(zero))) {
                    pxToAv.add(right);
                }
            }
        }
        int sum = 0;
        for (int[] in : pxToAv) {
            sum += in[0];
        }
        try {
            avVal = sum / pxToAv.size();
        } catch (ArithmeticException e) {
            e.printStackTrace();
        }
        return avVal;
    }
}
