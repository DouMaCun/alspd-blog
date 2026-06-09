package com.alspd.blog.controller;

import com.alspd.blog.common.ApiResponse;
import com.alspd.blog.common.PageResult;
import com.alspd.blog.domain.query.AdminPostQuery;
import com.alspd.blog.domain.request.AdminPostRequest;
import com.alspd.blog.domain.request.CategoryRequest;
import com.alspd.blog.domain.request.TagRequest;
import com.alspd.blog.domain.vo.AdminPostVO;
import com.alspd.blog.domain.vo.CategoryVO;
import com.alspd.blog.domain.vo.TagVO;
import com.alspd.blog.domain.vo.UploadVO;
import com.alspd.blog.service.BlogService;
import com.alspd.blog.service.UploadService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final BlogService blogService;
    private final UploadService uploadService;

    public AdminController(BlogService blogService, UploadService uploadService) {
        this.blogService = blogService;
        this.uploadService = uploadService;
    }

    @GetMapping("/posts")
    public ApiResponse<PageResult<AdminPostVO>> posts(AdminPostQuery query) {
        return ApiResponse.ok(blogService.listAdminPosts(query));
    }

    @GetMapping("/posts/{id}")
    public ApiResponse<AdminPostVO> post(@PathVariable Long id) {
        return ApiResponse.ok(blogService.getAdminPost(id));
    }

    @PostMapping("/posts")
    public ApiResponse<AdminPostVO> createPost(@RequestBody AdminPostRequest request) {
        return ApiResponse.ok(blogService.createAdminPost(request));
    }

    @PutMapping("/posts/{id}")
    public ApiResponse<AdminPostVO> updatePost(@PathVariable Long id, @RequestBody AdminPostRequest request) {
        return ApiResponse.ok(blogService.updateAdminPost(id, request));
    }

    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id) {
        blogService.deleteAdminPost(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryVO>> categories() {
        return ApiResponse.ok(blogService.listCategories());
    }

    @PostMapping("/categories")
    public ApiResponse<CategoryVO> createCategory(@RequestBody CategoryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("分类请求不能为空");
        }
        return ApiResponse.ok(blogService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<CategoryVO> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("分类请求不能为空");
        }
        return ApiResponse.ok(blogService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        blogService.deleteCategory(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/tags")
    public ApiResponse<List<TagVO>> tags() {
        return ApiResponse.ok(blogService.listTags());
    }

    @PostMapping("/tags")
    public ApiResponse<TagVO> createTag(@RequestBody TagRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("标签请求不能为空");
        }
        return ApiResponse.ok(blogService.createTag(request));
    }

    @PutMapping("/tags/{id}")
    public ApiResponse<TagVO> updateTag(@PathVariable Long id, @RequestBody TagRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("标签请求不能为空");
        }
        return ApiResponse.ok(blogService.updateTag(id, request));
    }

    @DeleteMapping("/tags/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable Long id) {
        blogService.deleteTag(id);
        return ApiResponse.ok(null);
    }

    @PostMapping(value = "/uploads/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadVO> uploadImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(uploadService.uploadImage(file));
    }
}
