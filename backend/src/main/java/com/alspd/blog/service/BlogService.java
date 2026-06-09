package com.alspd.blog.service;

import com.alspd.blog.common.PageResult;
import com.alspd.blog.domain.entity.Category;
import com.alspd.blog.domain.entity.Post;
import com.alspd.blog.domain.entity.PostTag;
import com.alspd.blog.domain.entity.Tag;
import com.alspd.blog.domain.query.AdminPostQuery;
import com.alspd.blog.domain.query.PostListQuery;
import com.alspd.blog.domain.request.AdminPostRequest;
import com.alspd.blog.domain.request.CategoryRequest;
import com.alspd.blog.domain.request.TagRequest;
import com.alspd.blog.domain.vo.AdminPostVO;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BlogService {

    private static final String STATUS_DRAFT = "DRAFT";
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

    public PageResult<AdminPostVO> listAdminPosts(AdminPostQuery query) {
        long current = normalizePage(query.getPage());
        long size = normalizeAdminSize(query.getSize());

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>();

        String status = normalizeNullableStatus(query.getStatus());
        if (status != null) {
            wrapper.eq(Post::getStatus, status);
        }

        String categorySlug = normalizeText(query.getCategorySlug());
        if (categorySlug != null) {
            Category category = findCategoryBySlug(categorySlug);
            if (category == null) {
                return PageResult.of(Collections.<AdminPostVO>emptyList(), 0, current, size, 0);
            }
            wrapper.eq(Post::getCategoryId, category.getId());
        }

        String keyword = normalizeText(query.getKeyword());
        if (keyword != null) {
            wrapper.and(item -> item
                    .like(Post::getTitle, keyword)
                    .or()
                    .like(Post::getSummary, keyword)
                    .or()
                    .like(Post::getSlug, keyword));
        }

        wrapper.orderByDesc(Post::getUpdatedAt)
                .orderByDesc(Post::getCreatedAt)
                .orderByDesc(Post::getId);

        Page<Post> postPage = postMapper.selectPage(new Page<Post>(current, size), wrapper);
        return PageResult.of(enrichAdminPosts(postPage.getRecords()),
                postPage.getTotal(),
                postPage.getCurrent(),
                postPage.getSize(),
                postPage.getPages());
    }

    public AdminPostVO getAdminPost(Long id) {
        Post post = getPostOrThrow(id);
        return enrichAdminPosts(Collections.singletonList(post)).get(0);
    }

    @Transactional
    public AdminPostVO createAdminPost(AdminPostRequest request) {
        Post post = new Post();
        applyPostRequest(post, request, true);
        postMapper.insert(post);
        replacePostTags(post.getId(), request.getTagIds());
        return getAdminPost(post.getId());
    }

    @Transactional
    public AdminPostVO updateAdminPost(Long id, AdminPostRequest request) {
        Post post = getPostOrThrow(id);
        applyPostRequest(post, request, false);
        postMapper.updateById(post);
        replacePostTags(post.getId(), request.getTagIds());
        return getAdminPost(post.getId());
    }

    @Transactional
    public void deleteAdminPost(Long id) {
        getPostOrThrow(id);
        postTagMapper.delete(new LambdaQueryWrapper<PostTag>()
                .eq(PostTag::getPostId, id));
        postMapper.deleteById(id);
    }

    @Transactional
    public CategoryVO createCategory(CategoryRequest request) {
        String name = requireText(request.getName(), "分类名称不能为空");
        String slug = requireSlug(request.getSlug(), "分类标识不能为空");
        ensureUniqueCategorySlug(slug, null);

        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(normalizeText(request.getDescription()));
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        categoryMapper.insert(category);
        return toCategoryVO(category);
    }

    @Transactional
    public CategoryVO updateCategory(Long id, CategoryRequest request) {
        Category category = getCategoryOrThrow(id);
        String name = requireText(request.getName(), "分类名称不能为空");
        String slug = requireSlug(request.getSlug(), "分类标识不能为空");
        ensureUniqueCategorySlug(slug, id);

        category.setName(name);
        category.setSlug(slug);
        category.setDescription(normalizeText(request.getDescription()));
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        categoryMapper.updateById(category);
        return toCategoryVO(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        getCategoryOrThrow(id);
        Long count = postMapper.selectCount(new LambdaQueryWrapper<Post>()
                .eq(Post::getCategoryId, id));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("该分类仍有关联文章，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    @Transactional
    public TagVO createTag(TagRequest request) {
        String name = requireText(request.getName(), "标签名称不能为空");
        String slug = requireSlug(request.getSlug(), "标签标识不能为空");
        ensureUniqueTagSlug(slug, null);

        Tag tag = new Tag();
        tag.setName(name);
        tag.setSlug(slug);
        tagMapper.insert(tag);
        return toTagVO(tag);
    }

    @Transactional
    public TagVO updateTag(Long id, TagRequest request) {
        Tag tag = getTagOrThrow(id);
        String name = requireText(request.getName(), "标签名称不能为空");
        String slug = requireSlug(request.getSlug(), "标签标识不能为空");
        ensureUniqueTagSlug(slug, id);

        tag.setName(name);
        tag.setSlug(slug);
        tagMapper.updateById(tag);
        return toTagVO(tag);
    }

    @Transactional
    public void deleteTag(Long id) {
        getTagOrThrow(id);
        postTagMapper.delete(new LambdaQueryWrapper<PostTag>()
                .eq(PostTag::getTagId, id));
        tagMapper.deleteById(id);
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

    private List<AdminPostVO> enrichAdminPosts(List<Post> posts) {
        if (CollectionUtils.isEmpty(posts)) {
            return Collections.emptyList();
        }

        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        Map<Long, CategoryVO> categories = loadCategories(posts);
        Map<Long, List<TagVO>> tagsByPostId = loadTags(postIds);

        List<AdminPostVO> result = new ArrayList<AdminPostVO>();
        for (Post post : posts) {
            List<TagVO> postTags = tagsByPostId.containsKey(post.getId())
                    ? tagsByPostId.get(post.getId())
                    : Collections.<TagVO>emptyList();

            AdminPostVO item = new AdminPostVO();
            item.setId(post.getId());
            item.setTitle(post.getTitle());
            item.setSlug(post.getSlug());
            item.setSummary(post.getSummary());
            item.setContent(post.getContent());
            item.setCoverImage(post.getCoverImage());
            item.setCategoryId(post.getCategoryId());
            item.setStatus(post.getStatus());
            item.setFeatured(post.getFeatured());
            item.setViewCount(post.getViewCount());
            item.setPublishedAt(post.getPublishedAt());
            item.setCreatedAt(post.getCreatedAt());
            item.setUpdatedAt(post.getUpdatedAt());
            item.setCategory(categories.get(post.getCategoryId()));
            item.setTags(postTags);
            item.setTagIds(postTags.stream().map(TagVO::getId).collect(Collectors.toList()));
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

    private void applyPostRequest(Post post, AdminPostRequest request, boolean creating) {
        if (request == null) {
            throw new IllegalArgumentException("文章请求不能为空");
        }
        String title = requireText(request.getTitle(), "文章标题不能为空");
        String slug = requireSlug(request.getSlug(), "文章标识不能为空");
        String summary = requireText(request.getSummary(), "文章摘要不能为空");
        String content = requireText(request.getContent(), "文章正文不能为空");
        String status = normalizeRequiredStatus(request.getStatus());

        ensureUniquePostSlug(slug, post.getId());
        ensureCategoryExists(request.getCategoryId());
        ensureTagsExist(request.getTagIds());

        post.setTitle(title);
        post.setSlug(slug);
        post.setSummary(summary);
        post.setContent(content);
        post.setCoverImage(normalizeText(request.getCoverImage()));
        post.setCategoryId(request.getCategoryId());
        post.setStatus(status);
        post.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
        post.setPublishedAt(resolvePublishedAt(status, request.getPublishedAt()));
        if (creating) {
            post.setViewCount(0);
        }
    }

    private void replacePostTags(Long postId, List<Long> tagIds) {
        postTagMapper.delete(new LambdaQueryWrapper<PostTag>()
                .eq(PostTag::getPostId, postId));
        if (CollectionUtils.isEmpty(tagIds)) {
            return;
        }

        for (Long tagId : new LinkedHashSet<Long>(tagIds)) {
            PostTag link = new PostTag();
            link.setPostId(postId);
            link.setTagId(tagId);
            postTagMapper.insert(link);
        }
    }

    private Post getPostOrThrow(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("文章 ID 不能为空");
        }
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        return post;
    }

    private Category getCategoryOrThrow(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("分类 ID 不能为空");
        }
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        return category;
    }

    private Tag getTagOrThrow(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("标签 ID 不能为空");
        }
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new IllegalArgumentException("标签不存在");
        }
        return tag;
    }

    private void ensureCategoryExists(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        if (categoryMapper.selectById(categoryId) == null) {
            throw new IllegalArgumentException("分类不存在");
        }
    }

    private void ensureTagsExist(List<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) {
            return;
        }
        Set<Long> distinctIds = new HashSet<Long>(tagIds);
        if (distinctIds.contains(null)) {
            throw new IllegalArgumentException("标签 ID 不能为空");
        }
        List<Tag> tags = tagMapper.selectBatchIds(distinctIds);
        if (tags.size() != distinctIds.size()) {
            throw new IllegalArgumentException("部分标签不存在");
        }
    }

    private void ensureUniquePostSlug(String slug, Long currentId) {
        Post existing = postMapper.selectOne(new LambdaQueryWrapper<Post>()
                .eq(Post::getSlug, slug)
                .last("LIMIT 1"));
        if (existing != null && (currentId == null || !existing.getId().equals(currentId))) {
            throw new IllegalArgumentException("文章标识已存在");
        }
    }

    private void ensureUniqueCategorySlug(String slug, Long currentId) {
        Category existing = findCategoryBySlug(slug);
        if (existing != null && (currentId == null || !existing.getId().equals(currentId))) {
            throw new IllegalArgumentException("分类标识已存在");
        }
    }

    private void ensureUniqueTagSlug(String slug, Long currentId) {
        Tag existing = findTagBySlug(slug);
        if (existing != null && (currentId == null || !existing.getId().equals(currentId))) {
            throw new IllegalArgumentException("标签标识已存在");
        }
    }

    private LocalDateTime resolvePublishedAt(String status, LocalDateTime publishedAt) {
        if (STATUS_PUBLISHED.equals(status)) {
            return publishedAt == null ? LocalDateTime.now() : publishedAt;
        }
        return publishedAt;
    }

    private String normalizeRequiredStatus(String status) {
        String safeStatus = normalizeNullableStatus(status);
        if (safeStatus == null) {
            return STATUS_DRAFT;
        }
        return safeStatus;
    }

    private String normalizeNullableStatus(String status) {
        String safeStatus = normalizeText(status);
        if (safeStatus == null) {
            return null;
        }
        safeStatus = safeStatus.toUpperCase(Locale.ROOT);
        if (!STATUS_DRAFT.equals(safeStatus) && !STATUS_PUBLISHED.equals(safeStatus)) {
            throw new IllegalArgumentException("文章状态只能是 DRAFT 或 PUBLISHED");
        }
        return safeStatus;
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

    private long normalizeAdminSize(long size) {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }

    private String requireText(String value, String message) {
        String safeValue = normalizeText(value);
        if (safeValue == null) {
            throw new IllegalArgumentException(message);
        }
        return safeValue;
    }

    private String requireSlug(String value, String message) {
        String slug = requireText(value, message);
        if (!slug.matches("^[a-zA-Z0-9][a-zA-Z0-9-_.]{1,199}$")) {
            throw new IllegalArgumentException("标识只能包含字母、数字、横线、下划线和点，且至少 2 个字符");
        }
        return slug;
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
