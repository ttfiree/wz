package com.lyc.wangzhan.utils;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ZipFileUtil {

    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(
            Arrays.asList(".jpg", ".jpeg", ".JPG", ".JPEG"));
    /**
     * 7z文件压缩
     *
     * @param inputFile  待压缩文件夹/文件名
     * @param outputFile 生成的压缩包名字
     */

    public static void zip7z(String inputFile, String outputFilePath, String title) throws Exception {
        File input = new File(inputFile);
        if (!input.exists()) {
            throw new Exception(input.getPath() + "待压缩文件不存在");
        }
        String outputFile = outputFilePath;
        SevenZOutputFile out = new SevenZOutputFile(new File(outputFile));
        compress(out, input, null);
        out.close();
    }

    /**
     * @param name 压缩文件名，可以写为null保持默认
     */
    //递归压缩
    public static void compress(SevenZOutputFile out, File input, String name) throws IOException {
        if (name == null) {
            name = input.getName();
        }
        SevenZArchiveEntry entry = null;
        //如果路径为目录（文件夹）
        if (input.isDirectory()) {
            //取出文件夹中的文件（或子文件夹）
            File[] flist = input.listFiles();

            if (flist.length == 0)//如果文件夹为空，则只需在目的地.7z文件中写入一个目录进入
            {
                entry = out.createArchiveEntry(input, name + "/");
                out.putArchiveEntry(entry);
            } else//如果文件夹不为空，则递归调用compress，文件夹中的每一个文件（或文件夹）进行压缩
            {
                for (int i = 0; i < flist.length; i++) {
                    compress(out, flist[i], name + "/" + flist[i].getName());
                }
            }
        } else//如果不是目录（文件夹），即为文件，则先写入目录进入点，之后将文件写入7z文件中
        {
            FileInputStream fos = new FileInputStream(input);
            BufferedInputStream bis = new BufferedInputStream(fos);
            entry = out.createArchiveEntry(input, name);
            out.putArchiveEntry(entry);
            int len = -1;
            //将源文件写入到7z文件中
            byte[] buf = new byte[1024];
            while ((len = bis.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            bis.close();
            fos.close();
            out.closeArchiveEntry();
        }
    }

    /**
     * 7z解压缩
     *
     * @param z7zFilePath 7z文件的全路径
     * @return 压缩包中所有的文件
     */
    public static Map<String, String> unZip7z(String z7zFilePath) {

        String un7zFilePath = "";        //压缩之后的绝对路径

        SevenZFile zIn = null;
        try {
            File file = new File(z7zFilePath);
            un7zFilePath = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".7z"));
            zIn = new SevenZFile(file);
            SevenZArchiveEntry entry = null;
            File newFile = null;
            while ((entry = zIn.getNextEntry()) != null) {
                //不是文件夹就进行解压
                if (!entry.isDirectory()) {
                    newFile = new File(un7zFilePath, entry.getName());
                    if (!newFile.exists()) {
                        new File(newFile.getParent()).mkdirs();   //创建此文件的上层目录
                    }
                    OutputStream out = new FileOutputStream(newFile);
                    BufferedOutputStream bos = new BufferedOutputStream(out);
                    int len = -1;
                    byte[] buf = new byte[(int) entry.getSize()];
                    while ((len = zIn.read(buf)) != -1) {
                        bos.write(buf, 0, len);
                    }
                    bos.flush();
                    bos.close();
                    out.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (zIn != null)
                    zIn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Map<String, String> resultMap = getFileNameList(un7zFilePath, "");
        return resultMap;
    }

    /**
     * 获取压缩包中的全部文件
     *
     * @param path 文件夹路径
     * @return 压缩包中所有的文件
     * @childName 每一个文件的每一层的路径==D==区分层数
     */
    private static Map<String, String> getFileNameList(String path, String childName) {
        System.out.println("path:" + path + "---childName:" + childName);
        Map<String, String> files = new HashMap<>();
        File file = new File(path); // 需要获取的文件的路径
        String[] fileNameLists = file.list(); // 存储文件名的String数组
        File[] filePathLists = file.listFiles(); // 存储文件路径的String数组
        for (int i = 0; i < filePathLists.length; i++) {
            if (filePathLists[i].isFile()) {
                files.put(fileNameLists[i] + "==D==" + childName, path + File.separator + filePathLists[i].getName());
            } else {
                files.putAll(getFileNameList(path + File.separator + filePathLists[i].getName(), childName + "&" + filePathLists[i].getName()));
            }
        }
        return files;
    }


}
