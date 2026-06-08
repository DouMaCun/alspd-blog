package com.alspd.blog.service;

import com.alspd.blog.common.PageResult;
import com.alspd.blog.domain.entity.Category;
import com.alspd.blog.domain.entity.Post;
import com.alspd.blog.domain.entity.PostTag;
import com.alspd.blog.domain.entity.Tag;
import com.alspd.blog.domain.query.PostListQuery;
import com.alspd.blog.domain.vo.BlogStatsVO;
import com.alspd.blog.domain.vo.CategoryVO;
import com.alspd.blog.domain.vo.PostDetailVO;
import com.alspd.blog.domain.vo.PostSummaryVO;
import com.alspd.blog.domain.vo.TagVO;
import com.alspd.blog.mapper.CategoryMapper;
import com.alspd.blog.mapper.PostMapper;
import com.alspd.blog.mapper.PostTagMapper;
import com.alspd.blog.mapper.TagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BlogService {

    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final PostMapper postMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final PostTagMapper postTagMapper;

    public BlogService(PostMapper postMapper,
                       CategoryMapper categoryMapper,
                       TagMapper tagMapper,
                       PostTagMapper postTagMapper) {
        this.postMapper = postMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.postTagMapper = postTagMapper;
    }

    public PageResult<PostSummaryVO> listPublishedPosts(PostListQuery query) {
        long current = normalizePage(query.getPage());
        long size = normalizeSize(query.getSize());

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, STATUS_PUBLISHED);

        String categorySlug = normalizeText(query.getCategorySlug());
        if (categorySlug != null) {
            Category category = findCategoryBySlug(categorySlug);
            if (category == null) {
                return emptyPage(current, size);
            }
            wrapper.eq(Post::getCategoryId, category.getId());
        }

        String tagSlug = normalizeText(query.getTagSlug());
        if (tagSlug != null) {
            Tag tag = findTagBySlug(tagSlug);
            if (tag == null) {
                return emptyPage(current, size);
            }
            List<PostTag> links = postTagMapper.selectList(new LambdaQueryWrapper<PostTag>()
                    .eq(PostTag::getTagId, tag.getId()));
            if (CollectionUtils.isEmpty(links)) {
                return emptyPage(current, size);
            }
            Set<Long> postIds = links.stream()
                    .map(PostTag::getPostId)
                    .collect(Collectors.toSet());
            wrapper.in(Post::getId, postIds);
        }

        String keyword = normalizeText(query.getKeyword());
        if (keyword != null) {
            wrapper.and(item -> item
                    .like(Post::getTitle, keyword)
                    .or()
                    .like(Post::getSummary, keyword)
                    .or()
                    .like(Post::getContent, keyword));
        }

        wrapper.orderByDesc(Post::getFeatured)
                .orderByDesc(Post::getPublishedAt)
                .orderByDesc(Post::getCreatedAt);

        Page<Post> postPage = postMapper.selectPage(new Page<Post>(current, size), wrapper);
        return PageResult.of(enrichSummaries(postPage.getRecords()),
                postPage.getTotal(),
                postPage.getCurrent(),
                postPage.getSize(),
                postPage.getPages());
    }

    public List<PostSummaryVO> listFeaturedPosts(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 6));
        List<Post> posts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, STATUS_PUBLISHED)
                .eq(Post::getFeatured, true)
                .orderByDesc(Post::getPublishedAt)
                .orderByDesc(Post::getCreatedAt)
                .last("LIMIT " + safeLimit));
        return enrichSummaries(posts);
    }

    public PostDetailVO getPublishedPostBySlug(String slug) {
        String safeSlug = normalizeText(slug);
        if (safeSlug == null) {
            throw new IllegalArgumentException("文章地址不能为空");
        }

        Post post = postMapper.selectOne(new LambdaQueryWrapper<Post>()
                .eq(Post::getSlug, safeSlug)
                .eq(Post::getStatus, STATUS_PUBLISHED)
                .last("LIMIT 1"));
        if (post == null) {
            throw new IllegalArgumentException("文章不存在或尚未发布");
        }

        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, post.getId())
                .setSql("view_count = view_count + 1"));
        post.setViewCount(post.getViewCount() == null ? 1 : post.getViewCount() + 1);

        PostSummaryVO summary = enrichSummaries(Collections.singletonList(post)).get(0);
        PostDetailVO detail = new PostDetailVO();
        detail.setId(summary.getId());
        detail.setTitle(summary.getTitle());
        detail.setSlug(summary.getSlug());
        detail.setSummary(summary.getSummary());
        detail.setCoverImage(summary.getCoverImage());
        detail.setCategory(summary.getCategory());
        detail.setTags(summary.getTags());
        detail.setFeatured(summary.getFeatured());
        detail.setViewCount(summary.getViewCount());
        detail.setPublishedAt(summary.getPublishedAt());
        detail.setCreatedAt(summary.getCreatedAt());
        detail.setContent(post.getContent());
        return detail;
    }

    public List<CategoryVO> listCategories() {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));
        return categories.stream().map(this::toCategoryVO).collect(Collectors.toList());
    }

    public List<TagVO> listTags() {
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                .orderByAsc(Tag::getName)
                .orderByAsc(Tag::getId));
        return tags.stream().map(this::toTagVO).collect(Collectors.toList());
    }

    public BlogStatsVO getStats() {
        BlogStatsVO stats = new BlogStatsVO();
        stats.setPostCount(postMapper.selectCount(new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, STATUS_PUBLISHED)));
        stats.setCategoryCount(categoryMapper.selectCount(null));
        stats.setTagCount(tagMapper.selectCount(null));
        stats.setTotalViews(sumPublishedViews());
        return stats;
    }

    private long sumPublishedViews() {
        QueryWrapper<Post> wrapper = new QueryWrapper<Post>()
                .select("COALESCE(SUM(view_count), 0)")
                .eq("status", STATUS_PUBLISHED);
        List<Object> values = postMapper.selectObjs(wrapper);
        if (CollectionUtils.isEmpty(values) || values.get(0) == null) {
            return 0;
        }
        Object value = values.get(0);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private List<PostSummaryVO> enrichSummaries(List<Post> posts) {
        if (CollectionUtils.isEmpty(posts)) {
            return Collections.emptyList();
        }

        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        Map<Long, CategoryVO> categories = loadCategories(posts);
        Map<Long, List<TagVO>> tagsByPostId = loadTags(postIds);

        List<PostSummaryVO> result = new ArrayList<PostSummaryVO>();
        for (Post post : posts) {
            PostSummaryVO item = new PostSummaryVO();
            item.setId(post.getId());
            item.setTitle(post.getTitle());
            item.setSlug(post.getSlug());
            item.setSummary(post.getSummary());
            item.setCoverImage(post.getCoverImage());
            item.setCategory(categories.get(post.getCategoryId()));
            item.setTags(tagsByPostId.containsKey(post.getId())
                    ? tagsByPostId.get(post.getId())
                    : Collections.<TagVO>emptyList());
            item.setFeatured(post.getFeatured());
            item.setViewCount(post.getViewCount());
            item.setPublishedAt(post.getPublishedAt());
            item.setCreatedAt(post.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    private Map<Long, CategoryVO> loadCategories(List<Post> posts) {
        Set<Long> categoryIds = posts.stream()
                .map(Post::getCategoryId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(categoryIds)) {
            return Collections.emptyMap();
        }

        List<Category> categories = categoryMapper.selectBatchIds(categoryIds);
        Map<Long, CategoryVO> result = new HashMap<Long, CategoryVO>();
        for (Category category : categories) {
            result.put(category.getId(), toCategoryVO(category));
        }
        return result;
    }

    private Map<Long, List<TagVO>> loadTags(List<Long> postIds) {
        List<PostTag> links = postTagMapper.selectList(new LambdaQueryWrapper<PostTag>()
                .in(PostTag::getPostId, postIds)
                .orderByAsc(PostTag::getId));
        if (CollectionUtils.isEmpty(links)) {
            return Collections.emptyMap();
        }

        Set<Long> tagIds = links.stream()
                .map(PostTag::getTagId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        if (CollectionUtils.isEmpty(tagIds)) {
            return Collections.emptyMap();
        }

        List<Tag> tags = tagMapper.selectBatchIds(tagIds);
        Map<Long, TagVO> tagMap = new HashMap<Long, TagVO>();
        for (Tag tag : tags) {
            tagMap.put(tag.getId(), toTagVO(tag));
        }

        Map<Long, List<TagVO>> result = new LinkedHashMap<Long, List<TagVO>>();
        for (PostTag link : links) {
            TagVO tag = tagMap.get(link.getTagId());
            if (tag == null) {
                continue;
            }
            if (!result.containsKey(link.getPostId())) {
                result.put(link.getPostId(), new ArrayList<TagVO>());
            }
            result.get(link.getPostId()).add(tag);
        }
        return result;
    }

    private Category findCategoryBySlug(String slug) {
        return categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getSlug, slug)
                .last("LIMIT 1"));
    }

    private Tag findTagBySlug(String slug) {
        return tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getSlug, slug)
                .last("LIMIT 1"));
    }

    private CategoryVO toCategoryVO(Category category) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setSlug(category.getSlug());
        vo.setDescription(category.getDescription());
        vo.setSortOrder(category.getSortOrder());
        return vo;
    }

    private TagVO toTagVO(Tag tag) {
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setSlug(tag.getSlug());
        return vo;
    }

    private PageResult<PostSummaryVO> emptyPage(long current, long size) {
        return PageResult.of(Collections.<PostSummaryVO>emptyList(), 0, current, size, 0);
    }

    private long normalizePage(long page) {
        return page < 1 ? 1 : page;
    }

    private long normalizeSize(long size) {
        if (size < 1) {
            return 8;
        }
        return Math.min(size, 30);
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
