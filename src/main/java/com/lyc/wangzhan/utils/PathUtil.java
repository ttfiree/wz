package com.lyc.wangzhan.utils;

import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.OutItemFactory;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class PathUtil {
    private static String path = "D:\\Bilibili";

    private static String outPath = "D:\\BiliBiliOutput";

    //创建图片路径
    public static String createImagePath(String cover,String title) {
        System.out.println("开始创建图片路径");
        //图片名称
        String imgName = cover.substring(cover.lastIndexOf("/") + 1);
        //创建路径
        File dir = new File(path);
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
        //使用title创建文件夹
        File titleDir = new File(dir + File.separator + title);
        if (!titleDir.exists()) {
            titleDir.mkdirs();
        }
        String fileName = titleDir + File.separator + movieName;
        System.out.println("视频路径：" + fileName);
        return fileName;
    }
    //将文件夹7z压缩
    public static void compressTo7z(String title) {
        String folderPath = path + File.separator + title;
        String outputPath = outPath + File.separator + title;
        File dir = new File(outputPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File[] files = folder.listFiles();
        if (files == null) {
            throw new IllegalArgumentException("Folder is empty or inaccessible");
        }

        try (RandomAccessFile raf = new RandomAccessFile(outputPath, "rw");
             IOutCreateArchive7z outArchive = SevenZip.openOutArchive7z()) {

            outArchive.setLevel(5); // Compression level (0-9)
            outArchive.createArchive(new RandomAccessFileOutStream(raf), files.length, new IOutCreateCallback<IOutItemAllFormats>() {

                @Override
                public void setOperationResult(boolean operationResultOk) {
                    // Handle operation result
                }

                @Override
                public IOutItemAllFormats getItemInformation(int index, OutItemFactory<IOutItemAllFormats> outItemFactory) {
                    IOutItemAllFormats item = outItemFactory.createOutItem();
                    item.setDataSize(files[index].length());
                    item.setPropertyPath(files[index].getName());
                    return item;
                }

                @Override
                public ISequentialInStream getStream(int index) throws SevenZipException {
                    return new ISequentialInStream() {
                        FileInputStream inputStream;

                        {
                            try {
                                inputStream = new FileInputStream(files[index]);
                            } catch (FileNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public int read(byte[] data) throws SevenZipException {
                            try {
                                return inputStream.read(data);
                            } catch (IOException e) {
                                throw new SevenZipException("Error reading file", e);
                            }
                        }

                        @Override
                        public void close() throws IOException {
                            inputStream.close();
                        }
                    };
                }

                @Override
                public void setTotal(long total) {
                    // Implement the method
                }

                @Override
                public void setCompleted(long complete) {
                    // Implement the method
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
