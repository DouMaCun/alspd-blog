# ALSPD Blog

个人技术博客网站，用来沉淀微服务设计、识别算法、嵌入式、AI 使用和开发感悟。

## 技术栈

- 后端：JDK 17、Spring Boot 3.4、MyBatis-Plus、MySQL
- 前端：Vue 3、Vite、lucide-vue-next
- 数据库表前缀：`blog_`
- 后端端口：`60002`
- 前端端口：`50002`

## 数据库

数据库你已创建为 `alspd-ai`。建表和示例数据脚本在 [sql/schema.sql](sql/schema.sql)，需要你手动执行。

如果 MySQL 账号不是 `root` 或有密码，请修改 [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml) 里的 `spring.datasource` 配置。

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

当前版本只开放公开阅读接口，避免直接暴露未鉴权的写接口。后续可以加带登录或管理令牌的文章管理后台。
