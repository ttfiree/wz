package com.lyc.wangzhan.utils;

import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.OutItemFactory;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class PathUtil {
    private static String path = "N:\\Bilibili";
    private static String pathPic = "N:\\Bilibilipic";

    private static String outPath = "N:\\BiliBiliOutput";

    //创建图片路径
    public static String createImagePath(String cover,String title) {
        System.out.println("开始创建图片路径");
        //图片名称
        String imgName = title + ".jpg";
        //创建路径
        File dir = new File(pathPic);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        //使用title创建文件夹
        File titleDir = new File(dir + File.separator + title);
        if (!titleDir.exists()) {
            titleDir.mkdirs();
        }
        String fileName = titleDir + File.separator + imgName;
        System.out.println("图片路径：" + fileName);
        return fileName;
    }

    // 创建视频路径
    public static String createMoviePath(String title) {
        System.out.println("开始创建视频路径");
        //图片名称
        String movieName = title + ".mp4";
        //创建路径
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        //title用-分割取第二个
        String[] titles = title.split("-");
        //使用title创建文件夹
        File titleDir = new File(dir + File.separator + titles[1]);
        if (!titleDir.exists()) {
            titleDir.mkdirs();
        }
        String fileName = titleDir + File.separator + movieName;
        System.out.println("视频路径：" + fileName);
        return fileName;
    }
    //将文件夹7z压缩
    public static void compressFolder(String title) {
        String folderPath = outPath + File.separator + title;
        String archivePath = outPath + File.separator + title + ".7z";
        try {
            ZipFileUtil.zip7z(folderPath, archivePath, title);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    //复制文件到指定路径
    public static void copyFile(String title) {
        String oldPath = "N:\\哔哩-Fun - 充电视频的搬运工.html";
        // 获取源文件名
        File oldFile = new File(oldPath);
        String fileName = oldFile.getName();

        String pathPicc = pathPic + File.separator + title;
        //遍历pathPic下的所有文件夹
        File file = new File(pathPicc);
        File[] files = file.listFiles();
        for (File file1 : files) {
            //获取文件夹名称
            String folderName = file1.getName();
            //将oldPath复制到pathPic下的文件夹
            String targetFilePath = pathPicc + File.separator + folderName+File.separator+"哔哩-Fun - 充电视频的搬运工.html";
            copy(oldFile,oldPath, targetFilePath);


        }
    }

    //移动视频到文件夹
    public static void moveFile(String title) {
        String oldPath = "N:\\BiliBiliOutput\\video";
        // 获取源文件名
        File oldFile = new File(oldPath);
        File[] files = oldFile.listFiles();
        //打开目标文件夹
        String pathPicc = pathPic + File.separator + title;
        File file2 = new File(pathPicc);
        File[] files1 = file2.listFiles();
        for (File file : files) {
            //获取文件名
            String fileName = file.getName();
            for (File file1 : files1) {
                if(fileName.contains(file1.getName())){
                    //将文件移动到pathPic下的文件夹
                    String targetFilePath = pathPicc + File.separator +file1.getName()+File.separator+ fileName;
                    moveUsingFilesMove(oldPath+ File.separator+fileName, targetFilePath);
                }
            }
        }
    }
    //移动文件
    /**
     * 复制文件的方法，优化了缓冲区大小和使用缓冲流
     *
     * @param oldFile 源文件对象
     * @param oldPath 源文件路径
     * @param newPath 目标文件路径
     */
    private static void move(File oldFile, String oldPath, String newPath) {
        final int BUFFER_SIZE = 16 * 1024; // 16KB 缓冲区
        try {
            if (oldFile.exists()) {
                try (BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(oldPath), BUFFER_SIZE);
                     BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(newPath), BUFFER_SIZE)) {

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = inStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead);
                    }
                    outStream.flush(); // 确保所有数据都写入
                }
            } else {
                throw new FileNotFoundException("源文件未找到: " + oldPath);
            }
        } catch (Exception e) {
            System.out.println("复制文件操作出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void moveUsingFilesMove(String oldPath, String newPath) {
        Path source = Paths.get(oldPath);
        Path target = Paths.get(newPath);

        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("文件已成功移动到: " + newPath);
        } catch (IOException e) {
            System.out.println("移动文件操作出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private static void copy(File oldFile,String oldPath, String newPath) {
        try {
            Path source = Paths.get(oldPath);
            Path target = Paths.get(newPath);

            try {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("文件已成功移动到: " + newPath);
            } catch (IOException e) {
                System.out.println("移动文件操作出错: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("复制文件操作出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
