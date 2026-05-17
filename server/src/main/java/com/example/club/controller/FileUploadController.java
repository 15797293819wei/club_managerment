package com.example.club.controller;

import com.example.club.common.ApiResponse;
import com.example.club.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 文件上传和访问接口
 */
@Tag(name = "文件管理", description = "文件上传和访问相关接口")
@RestController
@RequestMapping("/api/files")
public class FileUploadController {

	private final FileUploadService fileUploadService;

	public FileUploadController(FileUploadService fileUploadService) {
		this.fileUploadService = fileUploadService;
	}

	/**
	 * 上传Logo图片
	 */
	@Operation(summary = "上传Logo图片", description = "上传社团Logo图片，支持JPG、PNG格式，大小不超过5MB")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "上传成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "文件格式或大小不符合要求")
	})
	@PostMapping("/logos")
	public ApiResponse<Map<String, String>> uploadLogo(
		@Parameter(description = "Logo图片文件", required = true) @RequestParam("file") MultipartFile file) {
		try {
			String url = fileUploadService.uploadLogo(file);
			return ApiResponse.success(Map.of("url", url));
		} catch (IllegalArgumentException e) {
			return ApiResponse.error(400, e.getMessage());
		} catch (IOException e) {
			return ApiResponse.error(500, "文件上传失败：" + e.getMessage());
		}
	}

	/**
	 * 上传头像
	 */
	@Operation(summary = "上传用户头像", description = "上传用户头像图片，支持JPG、PNG格式，大小不超过3MB")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "上传成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "文件格式或大小不符合要求")
	})
	@PostMapping("/avatars")
	public ApiResponse<Map<String, String>> uploadAvatar(
		@Parameter(description = "头像图片文件", required = true) @RequestParam("file") MultipartFile file) {
		try {
			String url = fileUploadService.uploadAvatar(file);
			return ApiResponse.success(Map.of("url", url));
		} catch (IllegalArgumentException e) {
			return ApiResponse.error(400, e.getMessage());
		} catch (IOException e) {
			return ApiResponse.error(500, "文件上传失败：" + e.getMessage());
		}
	}

	/**
	 * 上传证明材料
	 */
	@Operation(summary = "上传证明材料", description = "上传社团创建证明材料，支持PDF、DOC、DOCX格式，大小不超过10MB")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "上传成功"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "文件格式或大小不符合要求")
	})
	@PostMapping("/attachments")
	public ApiResponse<Map<String, String>> uploadAttachment(
		@Parameter(description = "证明材料文件", required = true) @RequestParam("file") MultipartFile file) {
		try {
			String url = fileUploadService.uploadAttachment(file);
			return ApiResponse.success(Map.of("url", url));
		} catch (IllegalArgumentException e) {
			return ApiResponse.error(400, e.getMessage());
		} catch (IOException e) {
			return ApiResponse.error(500, "文件上传失败：" + e.getMessage());
		}
	}

	/**
	 * 访问上传的文件
	 */
	@Operation(summary = "访问文件", description = "根据文件路径访问上传的文件")
	@GetMapping("/{subDir}/{filename}")
	public ResponseEntity<Resource> getFile(
		@Parameter(description = "子目录", required = true) @PathVariable String subDir,
		@Parameter(description = "文件名", required = true) @PathVariable String filename) {
		try {
			Path filePath = Paths.get("uploads", subDir, filename);
			@SuppressWarnings("null")
			FileSystemResource resource = new FileSystemResource(filePath.toFile());
			
			if (!resource.exists()) {
				return ResponseEntity.notFound().build();
			}

			// 根据文件类型设置Content-Type
			String contentType = Files.probeContentType(filePath);
			if (contentType == null) {
				contentType = "application/octet-stream";
			}

			return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
				.body(resource);
		} catch (IOException e) {
			return ResponseEntity.notFound().build();
		}
	}
}

