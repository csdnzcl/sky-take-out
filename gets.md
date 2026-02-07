# 苍穹外卖学习
## day01
1. 完成了苍穹外卖的环境搭建，学习知识点：
+ **Nginx的反向代理机制**：在Nginx.conf中进行配置
    + 提高访问速度
    + 进行负载均衡--upstream webservers{server 服务器IP addr}
    + 保证后端服务安全--server{ location /api/ {proxy_pass 替换地址}}
+ **apifox**接口文档导入以及使用
+ **Maven**相关知识点中的继承
    + 子工程中<parent>中写相关坐标</parent>  父工程中<modules>子工程名</modules>
    + <properties>：进行版本控制，一般和<dependencyManagement>搭配使用
    <properties> 
    <lombok>1.18.30</lombok> 
    </properties>
    + <dependencyManagement>： 统一管理所有依赖版本，子模块只声明 groupId 和 artifactId
## day02
1. 员工管理
+ @RestController:
+ @RequestMapping("/admin/employee")：统一地址前缀
+ 对于分页查询---引入PageHelper依赖
    + 可专门定义一个DTO接收前端传来的分页要求--**EmployeePageQueryDTO**--包含分页相关信息
    同时定义一个PageResult，
    + Controller层：
    ```java
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO){
        PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO);
        return Result.success(pageResult);
    }
    ```
    + ServiceImpl层使用PageHelper
    ```java
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        pageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);
        long total = page.getTotal();
        List<Employee> records = page.getResult();
        return new PageResult(total, records);
    }
    ```
+ @RequestBody：当前端传参为json格式时，需要使用此注解
+ @PathVariable：修饰路径参数---eg: {id}
+ serviceImpl中实现新增员工：使用BeanUtils实现对象属性拷贝
```java
        //传给mapper层最好是实体类
        Employee employee = new Employee();
        //对象属性拷贝

        BeanUtils.copyProperties(employeeDTO, employee);

        //设置账号状态---默认启用（1），禁用（0）
        employee.setStatus(StatusConstant.ENABLE);
        //设置密码---默认123456
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        //设置创建时间和修改时间
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        //设置创建人ID和修改人ID
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.insert(employee);
```
## day03 菜品管理
1. 公共字段自动填充
+ 反射机制
+ 动态代理
+ AOP--面向切片的编程
  + 自定义一个注解annotation.AutoFill，用于标识需要填充的字段
```java
@Target(ElementType.METHOD)// 表示该注解用于方法上
@Retention(RetentionPolicy.RUNTIME)// 表示该注解在运行时生效
public @interface AutoFill {
    // 枚举值，用于指定填充数据的操作类型 Update,  Insert
    com.sky.enumeration.OperationType value();//包括UPDATE, INSERT
}
```
  + 自定义切面类，对加了AutoFill注解的方法进行AOP处理
```java
@Aspect // 表示当前类为切面类
@Component // 将当前类标记为组件--动态代理
@Slf4j
public class AutoFillAspect {
    /**
     * 指定切入点
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")//切入点表达式
    public void autoFillPointCut() {}

    /**
     * 前置通知：在通知中进行公共字段的赋值
     * @param joinPoint
     */
    @Before("autoFillPointCut()")//前置通知
    public void autoFill(JoinPoint joinPoint){
      //获取当前被拦截的方法上的数据库操作类型---不同类型操作，所对应的字段填充不同
      //获取方法签名
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();
      //获取方法上的注解对象
      AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
      //获取数据库操作类型
      OperationType operationType = autoFill.value() ;

      //获取到当前被拦截的方法参数--实体对象
      Object[] args = joinPoint.getArgs();
      if (args == null || args.length == 0) {
        return;
      }

      Object entity = args[0];


      //准备赋值的数据
      LocalDateTime now = LocalDateTime.now();
      Long currentId = BaseContext.getCurrentId();

      //根据当前不同的操作类型，为实体对象对应的属性通过 反射 赋值
      if(operationType == OperationType.INSERT){
        //为四个公共字段赋值
        try {
          Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
          //getClass() 获取当前被拦截的方法参数对应的实体类
          //getDeclaredMethod(方法名，方法中形参类型) 获取当前被拦截的方法参数对应的实体类中的方法
          Method setCreatUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
          //…………
          //通过反射为对象属性赋值
          setCreateTime.invoke(entity, now);
          //……
        } catch (Exception e) {
          e.printStackTrace();
        }
      }else if(operationType == OperationType.UPDATE){
        //……
      }
    }
}
```

## day04 菜品管理 + 设置店铺状态
1. *Mapper.xml中动态SQL写法:以苍穹外卖中菜品搜索接口为例
```SQL
<select id="listDishes" resultType="com.sky.entity.Dish">
    SELECT * FROM dish
    <where>
        <if test="name != null and name != ''">
            AND name LIKE concat('%', #{name}, '%')
        </if>
        
        <if test="categoryId != null">
            AND category_id = #{categoryId}
        </if>
        
        <if test="ids != null and ids.size > 0">
            AND id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
        </if>
    </where>
</select>

<update id="updateDish">
    UPDATE dish
    <set>
        <if test="name != null"> name = #{name}, </if>
        <if test="price != null"> price = #{price}, </if>
        <if test="image != null"> image = #{image}, </if>
    </set>
    WHERE id = #{id}
</update>

<!--注意：update中的set标签，里面的if需要加逗号-->
```
+ 知识点拆解：
  + where标签：写 WHERE 关键字 + 智能去除开头的 AND 或 OR（核心）
  + if 标签：String类型：既要判null,又要判断空串''
  + foreach标签：
    + collection="ids"：DTO 里那个List集合的属性名--变量名
    + item="id"：给遍历出来的每个元素起个临时变量名（下面 #{} 里就填这个）
    + open="("：循环开始前，加个左括号
    + close=")"：循环结束后，加个右括号
    + separator=","：每次循环中间，加个逗号。
    + 生成结果：(101, 102, 103)
2. Spring Data Redis使用步骤
+ 导入Maven坐标
+ 配置redis数据源
+ 编写配置类，创建RedisTemplate对象
+ 通过RedisTemplate操作Redis
```java
//编写配置类方法
@Configuration//配置类
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建RedisTemplate对象...");
        //创建RedisTemplate对象
        RedisTemplate redisTemplate = new RedisTemplate();
        //设置redis连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //设置redis key的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}
```
3. 使用alioss文件上传
  + 引入坐标 + 配置文件中进行配置
  + 创建阿里云文件上传工具类对象的bean
```java
/**
 * 阿里云文件上传配置
 * 用于创建AliOssUtil对象
 */

@Configuration
@Slf4j
public class OssConfiguration {

    @Bean
    @ConditionalOnMissingBean//当容器中没有这个bean时，创建这个bean
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
        log.info("开始创建阿里云文件上传工具类对象：{}", aliOssProperties);
        return new AliOssUtil(aliOssProperties.getEndpoint(), aliOssProperties.getAccessKeyId(), aliOssProperties.getAccessKeySecret(), aliOssProperties.getBucketName());
        //aliOssProperties中使用@ConfigurationProperties(prefix = "sky.alioss")//配置属性类，读取application.yml中的属性
    }
}
```
  + 开发文件上传接口
```java
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}", file);
        try {
            //原始文件名
            String originalFileName = file.getOriginalFilename();
            //截取原始文件名的后缀   dfdfdf.png
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            //构造新文件名称
            String objectName = UUID.randomUUID().toString() + extension;
            //文件请求路径
            String filePath = aliOssUtil.upload(file.getBytes(), objectName);
            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败：{}", e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }

}


```
4. DTO和VO
+ DTO (Data Transfer Object) 数据传输对象
      方向：前端 -> 后端 （主要是接收前端传来的参数）---Reason:前端传来数据往往和数据库表结构不一致
+ VO (View Object) 视图对象
    方向：后端 -> 前端 （主要是发给前端展示的数据） 作用：封装后端要展示给用户看的数据---Reason:数据库里的数据，往往不能直接给用户看，或者不够看。
5. @Transaction：开启事务---多表操作
6. @PathVariable 路径参数
7. HttpClien