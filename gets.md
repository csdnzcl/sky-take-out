# day01
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
# day02
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
# day03
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
    OperationType value();
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
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();//获取方法签名
      AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);//获取方法上的注解对象
      OperationType operationType = autoFill.value() ;//获取数据库操作类型

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