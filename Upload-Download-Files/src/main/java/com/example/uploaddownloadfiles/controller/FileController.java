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
            }
        }
        return new Msg(200, "上传成功！");
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
}
