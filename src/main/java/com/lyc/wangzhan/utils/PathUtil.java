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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PathUtil {
    private static String path = "N:\\Bilibili";
    private static String pathPic = "N:\\Bilibilipic";

    private static String outPath = "N:\\BiliBiliOutput";

    private static String quarkPath = "N:\\网站";

    private static String downLoadPath = "N:\\下载";
    private static String dealPath = "N:\\待压缩";

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
        //获取当前日期yyyy-MM-dd
        String date = LocalDate.now().toString();
        //在outPath + File.separator下创建日期文件夹
        File dateDir = new File(outPath + File.separator + date);
        if (!dateDir.exists()) {
            dateDir.mkdirs();
        }
        String archivePath = outPath + File.separator +date+File.separator+ title + ".7z";
        try {
            ZipFileUtil.zip7z(folderPath, archivePath, title);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static void compressFolderDownload(String title) {
        String folderPath = downLoadPath + File.separator + title;
        //获取当前日期yyyy-MM-dd
        String date = LocalDate.now().toString();
        //在outPath + File.separator下创建日期文件夹
        File dateDir = new File(dealPath + File.separator + date);
        if (!dateDir.exists()) {
            dateDir.mkdirs();
        }
        String archivePath = dealPath + File.separator +date+File.separator+ title + ".7z";
        String oldPath = "N:\\哔哩-Fun - 充电视频的搬运工.html";
        // 获取源文件名
        File oldFile = new File(oldPath);
        moveUsingFilesMove(oldPath,folderPath);
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
        moveFile(title, oldPath);

    }

    public static void moveFile(String title, String oldPath) {
        // 获取源文件名
        File oldFile = new File(oldPath);
        File[] files = oldFile.listFiles();
        //打开目标文件夹
        //获取当前日期yyyy-MM-dd
        String date = LocalDate.now().toString();
        //在outPath + File.separator下创建日期文件夹
        File dateDir = new File(outPath + File.separator + date);
        if (!dateDir.exists()) {
            dateDir.mkdirs();
        }
        String pathPicc = pathPic + File.separator +date+File.separator+ title;
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
    public static void moveFileQuark(String title) {
        // 获取源文件名
        String date = LocalDate.now().toString();
        String oldPath = dealPath+ File.separator + date;
        File oldFile = new File(dealPath+ File.separator + date+ File.separator + title+ ".7z");
        //在outPath + File.separator下创建日期文件夹
        File dateDir = new File(quarkPath + File.separator + date);
        if (!dateDir.exists()) {
            dateDir.mkdirs();
        }
        String pathPicc = quarkPath + File.separator +date;
        File file2 = new File(pathPicc);
                    //将文件移动到pathPic下的文件夹
                    String targetFilePath = pathPicc + File.separator +oldFile.getName()+ ".7z";
                    moveUsingFilesMove(oldPath+ File.separator+oldFile.getName()+ ".7z", targetFilePath);

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
