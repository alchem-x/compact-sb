# Compact Java App - 像写Python一样写Java

## 什么是Compact Java App？

Compact Java App是基于Java 25新特性（JEP 512）的编程方式，让你可以像写Python一样写Java程序：

```java
// 传统的Java Hello World
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}

// Compact Java App Hello World
void main() {
    IO.println("Hello, World!");
}
```

## 核心优势

✅ **零样板代码** - 无需类声明、public static void main等繁琐语法
✅ **自动导入** - 自动导入java.base所有常用类
✅ **简洁I/O** - 新的IO类简化控制台输入输出
✅ **平滑演进** - 可随时扩展到完整Java程序
✅ **原生性能** - 编译后与传统Java完全等效

## 当前项目介绍

这个仓库演示了Compact Java App的实际应用 - 一个轻量级的Web服务器实现。

### 项目结构

```
compact-sb/
├── Lu.java              # 紧凑Java App主程序
├── CompactSB.java       # Web服务器核心
├── lib/                 # 依赖库
└── README.md
```

### 快速开始

1. **确保Java 25+已安装**
```bash
java -version  # 需要Java 25或更高版本
```

2. **运行应用**
```bash
java Lu.java && java -cp "lib/*" CompactSB.java
```

3. **访问Web服务**
打开浏览器访问 http://localhost:8080/

### 核心代码示例

**Lu.java** - 紧凑Java App的精髓：
```java
void main() {
    // 简洁的Web服务器启动
    IO.println("Starting Compact Web Server...");

    // 自动导入所有java.base类
    var server = new Server();
    server.start(8080);

    IO.println("Server running at http://localhost:8080/");
}
```

### 为什么选择Compact Java App？

1. **教学友好** - 第一天就能写出实用的程序
2. **脚本化** - 用Java写脚本，享受强类型和优秀性能
3. **零配置** - 无需复杂项目结构和构建工具
4. **生产就绪** - 可平滑扩展到企业级应用

### 与传统Java对比

| 特性 | 传统Java | Compact Java App |
|------|----------|------------------|
| Hello World | 5行，4个概念 | 3行，1个概念 |
| 依赖管理 | 需要Maven/Gradle | 直接运行源文件 |
| 学习曲线 | 陡峭 | 平缓 |
| 开发速度 | 慢 | 快速 |
| 运行时性能 | 优秀 | **同样优秀** |

### 应用场景

- 🎓 **编程教学** - 让学生快速看到成果
- 🚀 **快速原型** - 验证想法的最佳选择
- ⚙️ **系统脚本** - 替代Shell脚本的安全选择
- 📊 **数据处理** - 处理CSV、JSON等数据文件
- 🌐 **Web服务** - 如本项目的轻量级服务器

### 技术细节

- **隐式类声明** - 源文件自动转换为final类
- **实例main方法** - 无需static修饰符
- **自动导入** - java.base模块54个包自动可用
- **IO简化** - `IO.println()`替代`System.out.println()`

### 下一步计划

这个项目展示了Compact Java App在Web开发中的应用。未来可以：

1. 添加更多路由处理功能
2. 集成模板引擎
3. 支持静态文件服务
4. 添加数据库连接示例
5. 创建RESTful API演示

### 相关资源

- [JEP 512官方文档](https://openjdk.org/jeps/512) - 技术规范
- [JEP 512中文翻译](jep-512-bilingual.md) - 双语版本
- [OpenJDK 25下载](https://jdk.java.net/25/) - 获取Java 25

---

**Compact Java App让Java重新变得简单优雅，同时保持其强大的生态系统。未来已来，让我们一起拥抱这个全新的Java时代！** 🚀

*本项目是Compact Java App理念的实践演示，展示了如何用简洁的代码构建实用的应用程序。*