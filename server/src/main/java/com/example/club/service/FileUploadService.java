package com.example.club.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 文件上传服务
 * 负责处理文件上传、存储和访问路径生成
 */
@Service
public class FileUploadService {

	@Value("${app.upload.dir:uploads}")
	private String uploadDir;

	@Value("${app.upload.url-prefix:/api/files}")
	private String urlPrefix;

	/**
	 * 上传Logo图片文件
	 * @param file 上传的文件
	 * @return 文件的访问URL
	 * @throws IOException 文件操作异常
	 */
	public String uploadLogo(MultipartFile file) throws IOException {
		// 验证文件类型
		String contentType = file.getContentType();
		if (contentType == null || (!contentType.startsWith("image/jpeg") && !contentType.startsWith("image/png"))) {
			throw new IllegalArgumentException("Logo文件必须是JPG或PNG格式");
		}
		// 验证文件大小（5MB）
		if (file.getSize() > 5 * 1024 * 1024) {
			throw new IllegalArgumentException("Logo文件大小不能超过5MB");
		}
		return uploadFile(file, "logos");
	}

	/**
	 * 上传头像图片文件
	 * @param file 上传的文件
	 * @return 文件访问URL
	 * @throws IOException 文件操作异常
	 */
	public String uploadAvatar(MultipartFile file) throws IOException {
		String contentType = file.getContentType();
		if (contentType == null || (!contentType.startsWith("image/jpeg") && !contentType.startsWith("image/png"))) {
			throw new IllegalArgumentException("头像必须是JPG或PNG格式");
		}
		// 限制头像大小 3MB
		if (file.getSize() > 3 * 1024 * 1024) {
			throw new IllegalArgumentException("头像文件大小不能超过3MB");
		}
		return uploadFile(file, "avatars");
	}

	/**
	 * 上传证明材料文件
	 * @param file 上传的文件
	 * @return 文件的访问URL
	 * @throws IOException 文件操作异常
	 */
	public String uploadAttachment(MultipartFile file) throws IOException {
		// 验证文件类型
		String contentType = file.getContentType();
		if (contentType == null || (!contentType.equals("application/pdf") 
			&& !contentType.equals("application/msword")
			&& !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
			throw new IllegalArgumentException("证明材料文件必须是PDF、DOC或DOCX格式");
		}
		// 验证文件大小（10MB）
		if (file.getSize() > 10 * 1024 * 1024) {
			throw new IllegalArgumentException("证明材料文件大小不能超过10MB");
		}
		return uploadFile(file, "attachments");
	}

	/**
	 * 通用文件上传方法
	 * @param file 上传的文件
	 * @param subDir 子目录（如：logos, attachments）
	 * @return 文件的访问URL
	 * @throws IOException 文件操作异常
	 */
	private String uploadFile(MultipartFile file, String subDir) throws IOException {
		// 创建上传目录
		Path uploadPath = Paths.get(uploadDir, subDir);
		if (!Files.exists(uploadPath)) {
			Files.createDirectories(uploadPath);
		}

		// 生成唯一文件名
		String originalFilename = file.getOriginalFilename();
		String extension = "";
		if (originalFilename != null && originalFilename.contains(".")) {
			extension = originalFilename.substring(originalFilename.lastIndexOf("."));
		}
		String filename = UUID.randomUUID().toString() + extension;

		// 保存文件
		Path filePath = uploadPath.resolve(filename);
		Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

		// 返回访问URL
		return urlPrefix + "/" + subDir + "/" + filename;
	}

	/**
	 * 删除文件
	 * @param fileUrl 文件的访问URL
	 * @return 是否删除成功
	 */
	public boolean deleteFile(String fileUrl) {
		if (fileUrl == null || !fileUrl.startsWith(urlPrefix)) {
			return false;
		}
		try {
			// 从URL中提取文件路径
			String relativePath = fileUrl.substring(urlPrefix.length() + 1);
			Path filePath = Paths.get(uploadDir, relativePath);
			return Files.deleteIfExists(filePath);
		} catch (IOException e) {
			return false;
		}
	}
}

