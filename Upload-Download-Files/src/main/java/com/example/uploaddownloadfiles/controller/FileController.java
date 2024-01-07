package com.example.uploaddownloadfiles.controller;

import demos.web.Msg;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Map;
// 使用jmatio库读取.mat文件
import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

@RestController
@CrossOrigin
public class FileController {
    @Value("${file.path}")
    private String path;

    @RequestMapping("/upload")
    public Msg upload(MultipartFile[] files) throws IOException {
        Msg msg = new Msg();

        if (files == null || files.length == 0) {
            msg.setCode(500);
            msg.setMsg("空文件！");
            return msg;
        }

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String filename = file.getOriginalFilename();
                if (!filename.endsWith(".mat")) {
                    msg.setCode(500);
                    msg.setMsg("失败，请上传.mat文件！");
                    return msg;
                }

                File storeFile = new File(path + filename);
                if (storeFile.exists()) {
                    msg.setCode(500);
                    msg.setMsg("文件已经存在！");
                    return msg;
                }
                file.transferTo(storeFile);
                // 调用算法进行分类处理
                classify(storeFile);
            }
        }
        return new Msg(200, "上传成功！");
    }

    // 定义一个方法，用于调用Python算法进行分类处理
    public void classify(File file) throws IOException {
        // 算法叫做demo.py，位于path目录下，接受一个文件名作为输入，输出一个分类结果文件，命名为result.mat
        // 使用Runtime和ProcessBuilder执行Python代码
        Runtime runtime = Runtime.getRuntime();
        ProcessBuilder pb = new ProcessBuilder("python", path + "demo.py", file.getName());
        Process process = pb.start();
        // 等待Python代码执行完成
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/download")
    public String download(HttpServletResponse resp, String filename) throws IOException {
        File file = new File(path + filename);
        if (!file.exists()) return "文件不存在！";

        resp.reset();
        resp.setContentType("application/force-download");
        resp.setCharacterEncoding("utf-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Content-Disposition", "attachment;filename=" + filename);

        byte[] readBytes = FileCopyUtils.copyToByteArray(file);
        OutputStream os = resp.getOutputStream();
        os.write(readBytes);

        return "下载成功！";
    }

    @RequestMapping("/show")
    public String show(HttpServletResponse resp, String filename) throws IOException {
        // 定义一个方法，用于展示分类的结果图像
        // .mat文件，假设其中有一个键叫做'img'，对应的值是一个二维数组，表示图像的像素值

        File file = new File(path + filename);
        if (!file.exists()) return "文件不存在！";
        // 读取.mat文件中的图像数据
        MatFileReader reader = new MatFileReader(file);
        MLArray mlArray = reader.getMLArray("img");
        MLDouble d = (MLDouble) mlArray;
        double[][] img = d.getArray();
        // 使用BufferedImage类创建一个图像对象
        BufferedImage image = new BufferedImage(img.length, img[0].length, BufferedImage.TYPE_INT_RGB);
        // 将图像数据写入图像对象中
        for (int i = 0; i < img.length; i++) {
            for (int j = 0; j < img[0].length; j++) {
                // 将灰度值转换为RGB值
                int gray = (int) img[i][j];
                int rgb = (gray << 16) | (gray << 8) | gray;
                // 将RGB值写入图像对象中
                image.setRGB(i, j, rgb);
            }
        }
        // 将图像对象写入响应流中
        resp.reset();
        resp.setContentType("image/jpeg");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        // 使用ImageIO类将图像对象写入响应流
        ImageIO.write(image, "jpg", resp.getOutputStream());
        return "展示成功！";
    }
}