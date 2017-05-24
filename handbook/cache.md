# 缓存中间件 - sedis #

缓存，作为数据的中间层，承载了重要的使命，保护数据层，提高接口的响应速度，等等。但缓存层和数据层之间的联系，比较脆弱，尤其是分布式缓存的明星产品，redis，与数据库之间隔着网络层，联系更为脆弱。

为了维护缓存层和数据层的紧密，需要解决很多缓存问题，例如恰当的缓存更新策略、缓存粒度、缓存穿透等等，代码显然比较复杂，如果和业务代码耦合在一起，更是难以维护。

这就是sedis存在的意义。它使用动态代理，封装了所有的缓存操作，与业务代码做到了百分之百的隔离。

### 功能简介 ###

- 可用性很强，只需要在目标方法添加注解，不需要一行缓存代码，对应用完全透明；
- 利用注解(@Cache[增查合一]，@CacheExpire，@CacheUpdate)，实现缓存的增删改查，其中增删查为实时，改为准实时；
- 缓存采用三级缓存，分别是memory - redis - datasource，且每一级均为可选；
- 缓存粒度为单JVM单key，避免缓存热点问题；
- 缓存层与应用处于同一个JVM，不会出现雪崩现象；
- 提供简单的监控界面。

### 使用文档 ###

- **简单示例**

1. 引入jar包

```xml
<dependency>
    <groupId>org.sedis</groupId>
    <artifactId>sedis</artifactId>
    <version>1.0.0.RELEASE</version>
</dependency>
```

2. 配置spring-context.xml文件

```xml
<!-- 声明sedis标签,表示开启缓存中间件功能 -->
<sedis:annotation-driven sedis-client="sedisClient"/>

<!-- 声明redis连接池,可以声明多个,组建集群,注入ShardedJedisPool -->
<bean id="jedisPoolConfig" class="redis.clients.jedis.JedisPoolConfig">
    <property name="maxIdle" value="10"/>
    <property name="testOnBorrow" value="true"/>
</bean>

<!-- 声明redis集群客户端 -->
<bean id="sedisClient" class="redis.clients.jedis.ShardedJedisPool">
    <constructor-arg ref="jedisPoolConfig"/>
    <constructor-arg>
        <list>
            <bean class="redis.clients.jedis.JedisShardInfo">
                <constructor-arg value="127.0.0.1"/>
                <constructor-arg type="int" value="6379"/>
                <constructor-arg value="123456"/>
            </bean>
        </list>
    </constructor-arg>
</bean>
```

3. 准备测试类文件
	
```java
public class Waybill {

    private String waybillCode;
    private Integer waybillStatus;

    public Waybill() {
    }

    public Waybill(String waybillCode, Integer waybillStatus) {
        this.waybillCode = waybillCode;
        this.waybillStatus = waybillStatus;
    }

    public String getWaybillCode() {
        return waybillCode;
    }

    public void setWaybillCode(String waybillCode) {
        this.waybillCode = waybillCode;
    }

    public Integer getWaybillStatus() {
        return waybillStatus;
    }

    public void setWaybillStatus(Integer waybillStatus) {
        this.waybillStatus = waybillStatus;
    }

    @Override
    public String toString() {
        return "Waybill{" +
                "waybillCode='" + waybillCode + '\'' +
                ", waybillStatus=" + waybillStatus +
                '}';
    }
}
```
	
```java
public interface WaybillService {
    Waybill findById(String code);
    int updateById(String code);
    int deleteById(String code);
}

public class WaybillServiceImpl implements WaybillService {
	@Override
    @Cache(redisEnable = true, key = "waybill@args0")
    public Waybill findById(String code) {
        return new Waybill(code, new Random().nextInt(1000));
    }
    @Override
    @CacheExpire(key = "waybill@args0")
    public int deleteById(String code) {
        return 1;
    }
    @Override
    @CacheExpire(key = "waybill@args0")
    public int updateById(String code) {
        return 1;
    }
}
```

```java
public class SedisCacheTest {
	public static void main(String[] args) {
		String configPath = "classpath:spring-context.xml";
		ApplicationContext context = new ClassPathXmlApplicationContext(configPath);
		WaybillService service = (WaybillService) context.getBean("waybillService");

		String code = "1";
		long s13 = System.currentTimeMillis();
		System.out.println("findById, " + service.findById(code));
		long e13 = System.currentTimeMillis();
		System.out.println("f13 is " + (e13 - s13));
		long s14 = System.currentTimeMillis();
		System.out.println("findById, " + service.findById(code));
		long e14 = System.currentTimeMillis();
		System.out.println("f14 is " + (e14 - s14));

		System.out.println("==================================");

	    long s16 = System.currentTimeMillis();
	    Waybill beforeWaybill = service.findById(code);
	    System.out.println("updateById, " + service.updateById(code));
	    Waybill afterWaybill = service.findById(code);
	    System.out.println("beforeWaybill.waybillStatus == " + beforeWaybill.getWaybillStatus() //
	            + ", afterWaybill.waybillStatus == " + afterWaybill.getWaybillStatus());
	    long e16 = System.currentTimeMillis();
	    System.out.println("f16 is " + (e16 - s16));
	
	    System.out.println("==================================");
	
	    long s17 = System.currentTimeMillis();
	    Waybill beforeDeleteWaybill = service.findById(code);
	    System.out.println("updateById, " + service.deleteById(code));
	    Waybill afterDeleteWaybill = service.findById(code);
	    System.out.println("beforeDeleteWaybill.waybillStatus == " + beforeDeleteWaybill.getWaybillStatus() //
	            + ", afterDeleteWaybill.waybillStatus == " + afterDeleteWaybill.getWaybillStatus());
	    long e17 = System.currentTimeMillis();
	    System.out.println("f17 is " + (e17 - s17));
	
	    System.out.println("==================================");
	}
}
```

注意：在业务代码中，使用缓存，只需要在获取的目标方法，添加@Cache注解即可。key的语法为"全局唯一字符串@参数列表(参数用@隔开)"。如果上面的接口有多个参数，那么应该写成这样，`waybillService.findById@args0@args1@args2`，其中"waybillService.findById"在整个应用中，保持唯一。

- **用户指南**

sedis所有的缓存特性，只有两个地方可以设置，一个是spring标签`<sedis:annotation-driven>`，另一个是注解`@Cache`、`@CacheExpire`、`@CacheUpdate`，尽可能提高产品的易用性。

`<sedis:annotation-driven>`目前支持的属性：

- `sedis-client`：redis客户端实现，目前为`redis.clients.jedis.ShardedJedisPool`；
- `proxy-target-class`：false表示使用JDK动态代理,true表示使用CGLIB动态代理，默认false；
- `memory-count`：内存缓存最大容量，默认10000；
- `lock-count`：缓存处理器的加锁数量,使用锁避免相同key的并发问题，可以使用默认值10000；
- `max-period`：缓存处理器的锁的生存周期长度，默认1小时，单位毫秒；
- `delay`：执行缓存处理器中的锁的清理任务的执行间隔，默认1小时，单位毫秒；

除了`sedis-client`是必须要设置的，因为此产品的核心功能，就是基于redis的缓存服务。虽然也提供内存缓存，但仅推荐少量数据的缓存场景，比如配置。

注解`@Cache`目前支持的属性：

- `key`：一种表达式，缓存当前接口数据的key，格式为`全局唯一字符串@参数列表（参数用@隔开）`，例如`xxxService.getOrder@args0@args1@args2`；
- `memoryEnable`：是否支持内存缓存，true表示支持，false表示不支持，默认false；
- `memoryExpiredTime`：内存缓存的过期时间，默认半小时，单位毫秒；
- `redisEnable`：是否支持redis缓存，true表示支持，false表示不支持，默认false；
- `redisExpiredTime`：redis缓存的过期时间，默认一小时，单位毫秒；
- `dataSourceEnable`：是否需要从数据层获取原始数据，默认为true，表示支持，建议使用默认配置。

注解`@CacheExpire`目前支持的属性：

- `key`：必须与对应的`@Cache`的`key`保持一致；
- `memoryEnable`：是否支持内存缓存，true表示支持，false表示不支持，默认false；
- `redisEnable`：是否支持redis缓存，true表示支持，false表示不支持，默认false；
- `dataSourceEnable`：是否需要删除原始数据层，默认为true，表示支持，建议使用默认配置。

注解`@CacheUpdate`目前支持的属性：

- `key`：必须与对应的`@Cache`的`key`保持一致；
- `memoryEnable`：是否支持内存缓存，true表示支持，false表示不支持，默认false；
- `redisEnable`：是否支持redis缓存，true表示支持，false表示不支持，默认false；
- `dataSourceEnable`：是否需要更新原始数据层，默认为true，表示支持，建议使用默认配置。

> 提醒
> `@Cache`、`@CacheExpire`、`@CacheUpdate`，这一套注解，`key`和`参数列表`，必须保持一致。参考测试类`WaybillServiceImpl`。
























































