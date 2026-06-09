# ALSPD Blog

个人技术博客网站，用来沉淀微服务设计、识别算法、嵌入式、AI 使用和开发感悟。

当前版本：Version 2，包含公开阅读站点和文章管理后台。

## 技术栈

- 后端：JDK 17、Spring Boot 3.4、MyBatis-Plus、MySQL
- 前端：Vue 3、Vite、lucide-vue-next
- 数据库表前缀：`blog_`
- 后端端口：`60002`
- 前端端口：`50002`

## 数据库

数据库你已创建为 `alspd-ai`。建表和示例数据脚本在 [sql/schema.sql](sql/schema.sql)，需要你手动执行。

如果 MySQL 账号不是 `root` 或有密码，请修改 [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml) 里的 `spring.datasource` 配置。

## 管理后台

后台入口：`http://localhost:50002/admin`

管理接口统一使用请求头 `X-Admin-Token`。默认令牌配置在 [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml)：

```yaml
blog:
  admin-token: please-change-me
```

本地运行前建议改成自己的值。前端后台首次进入会要求输入管理令牌，并保存在浏览器 `localStorage`。

后台发布文章时支持上传本地图片：

- 封面图：在文章表单的“封面图”字段点击“上传”
- 正文图：在“正文”编辑区点击“插入图片”，会自动插入 `![图片名](/uploads/images/...)`
- 后端默认保存目录：`backend/uploads/images`，该目录已被 `.gitignore` 忽略
- 图片访问路径：`/uploads/images/...`
- 单文件上传上限：`10MB`

## 本地启动

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm.cmd install --cache ../.npm-cache
npm.cmd run dev
```

访问：`http://localhost:50002`

## 当前接口

- `GET /api/health`
- `GET /api/posts?page=1&size=6&keyword=&categorySlug=&tagSlug=`
- `GET /api/posts/featured?limit=4`
- `GET /api/posts/{slug}`
- `GET /api/categories`
- `GET /api/tags`
- `GET /api/stats`

## 管理接口

- `GET /api/admin/posts?page=1&size=10&keyword=&status=&categorySlug=`
- `GET /api/admin/posts/{id}`
- `POST /api/admin/posts`
- `PUT /api/admin/posts/{id}`
- `DELETE /api/admin/posts/{id}`
- `GET /api/admin/categories`
- `POST /api/admin/categories`
- `PUT /api/admin/categories/{id}`
- `DELETE /api/admin/categories/{id}`
- `GET /api/admin/tags`
- `POST /api/admin/tags`
- `PUT /api/admin/tags/{id}`
- `DELETE /api/admin/tags/{id}`
- `POST /api/admin/uploads/images`
