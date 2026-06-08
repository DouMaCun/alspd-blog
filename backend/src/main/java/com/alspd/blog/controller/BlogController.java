package com.alspd.blog.controller;

import com.alspd.blog.common.ApiResponse;
import com.alspd.blog.common.PageResult;
import com.alspd.blog.domain.query.PostListQuery;
import com.alspd.blog.domain.vo.BlogStatsVO;
import com.alspd.blog.domain.vo.CategoryVO;
import com.alspd.blog.domain.vo.PostDetailVO;
import com.alspd.blog.domain.vo.PostSummaryVO;
import com.alspd.blog.domain.vo.TagVO;
import com.alspd.blog.service.BlogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Collections.singletonMap("status", "UP"));
    }

    @GetMapping("/posts")
    public ApiResponse<PageResult<PostSummaryVO>> posts(PostListQuery query) {
        return ApiResponse.ok(blogService.listPublishedPosts(query));
    }

    @GetMapping("/posts/featured")
    public ApiResponse<List<PostSummaryVO>> featuredPosts(@RequestParam(defaultValue = "4") int limit) {
        return ApiResponse.ok(blogService.listFeaturedPosts(limit));
    }

    @GetMapping("/posts/{slug}")
    public ApiResponse<PostDetailVO> postDetail(@PathVariable String slug) {
        return ApiResponse.ok(blogService.getPublishedPostBySlug(slug));
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryVO>> categories() {
        return ApiResponse.ok(blogService.listCategories());
    }

    @GetMapping("/tags")
    public ApiResponse<List<TagVO>> tags() {
        return ApiResponse.ok(blogService.listTags());
    }

    @GetMapping("/stats")
    public ApiResponse<BlogStatsVO> stats() {
        return ApiResponse.ok(blogService.getStats());
    }
}
