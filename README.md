# 一主一从三哨兵
哨兵模式下可以不用配置spring.redis.host和spring.redis.port
只配置spring.redis.sentinel的master和nodes即可
哨兵启动后，互相保持通信，一个哨兵挂掉会从其他哨兵获取最新信后，三个哨兵需要选举一个leader进行主从转移

## 持久化
Redis有两种持久化的方式：快照（RDB文件）和追加式文件（AOF文件）
- RDB持久化方式会在一个特定的间隔保存那个时间点的一个数据快照。
- AOF持久化方式则会记录每一个服务器收到的写操作。在服务启动时，这些记录的操作会逐条执行从而重建出原来的数据。写操作命令记录的格式跟Redis协议一致，以追加的方式进行保存。
- Redis的持久化是可以禁用的，就是说你可以让数据的生命周期只存在于服务器的运行时间里。
- 两种方式的持久化是可以同时存在的，但是当Redis重启时，AOF文件会被优先用于重建数据。
### rdb
RDB是在某个时间点将数据写入一个临时文件，持久化结束后，用这个临时文件替换上次持久化的文件，达到数据恢复。
优点：使用单独子进程来进行持久化，主进程不会进行任何IO操作，保证了redis的高性能
缺点：RDB是间隔一段时间进行持久化，如果持久化之间redis发生故障，会发生数据丢失。所以这种方式更适合数据要求不严谨的时候
RDB是Redis默认的持久化方式，所以RDB是默认开启的

``` shell
#dbfilename：持久化数据存储在本地的文件
dbfilename dump.rdb
#dir：持久化数据存储在本地的路径，如果是在/redis/redis-3.0.6/src下启动的redis-cli，则数据会存储在当前src目录下
dir ./
##snapshot触发的时机，save <seconds> <changes>  
##如下为900秒后，至少有一个变更操作，才会snapshot  
##对于此值的设置，需要谨慎，评估系统的变更操作密集程度  
##可以通过“save “””来关闭snapshot功能  
#save时间，以下分别表示更改了1个key时间隔900s进行持久化存储；更改了10个key300s进行存储；更改10000个key60s进行存储。
save 900 1
save 300 10
save 60 10000
##当snapshot时出现错误无法继续时，是否阻塞客户端“变更操作”，“错误”可能因为磁盘已满/磁盘故障/OS级别异常等  
stop-writes-on-bgsave-error yes  
##是否启用rdb文件压缩，默认为“yes”，压缩往往意味着“额外的cpu消耗”，同时也意味这较小的文件尺寸以及较短的网络传输时间  
rdbcompression yes  
```

如果想禁用快照保存的功能，可以通过注释掉所有"save"配置达到，或者在最后一条"save"配置后添加如下的配置： save ""
### aof
快照并不是很可靠。如果你的电脑突然宕机了，或者电源断了，又或者不小心杀掉了进程，那么最新的数据就会丢失。而AOF文件则提供了一种更为可靠的持久化方式。每当Redis接受到会修改数据集的命令时，就会把命令追加到AOF文件里，当你重启Redis时，AOF里的命令会被重新执行一次，重建数据。
Redis的AOF持久化策略是将发送到Redis服务端的每一条命令都记录下来,并且保存在硬盘的AOF文件中。可以通过参数appendonly来设置是否启用AOF。AOF文件的位置和RDB的位置相同,都是通过dir参数设置,默认的文件名是appendonly.aof,可以通过appendfilename参数修改。
将“操作 + 数据”以格式化指令的方式追加到操作日志文件的尾部，在append操作返回后(已经写入到文件或者即将写入)，才进行实际的数据变更当server需要数据恢复时，可以直接replay此日志文件，即可还原所有的操作过程。AOF相对可靠

 

优点：

比RDB可靠。你可以制定不同的fsync策略：不进行fsync、每秒fsync一次和每次查询进行fsync。默认是每秒fsync一次。这意味着你最多丢失一秒钟的数据。
AOF日志文件是一个纯追加的文件。就算是遇到突然停电的情况，也不会出现日志的定位或者损坏问题。甚至如果因为某些原因（例如磁盘满了）命令只写了一半到日志文件里，我们也可以用redis-check-aof这个工具很简单的进行修复。
当AOF文件太大时，Redis会自动在后台进行重写。重写很安全，因为重写是在一个新的文件上进行，同时Redis会继续往旧的文件追加数据。新文件上会写入能重建当前数据集的最小操作命令的集合。当新文件重写完，Redis会把新旧文件进行切换，然后开始把数据写到新文件上。
AOF把操作命令以简单易懂的格式一条接一条的保存在文件里，很容易导出来用于恢复数据。例如我们不小心用FLUSHALL命令把所有数据刷掉了，只要文件没有被重写，我们可以把服务停掉，把最后那条命令删掉，然后重启服务，这样就能把被刷掉的数据恢复回来。

缺点：

在相同的数据集下，AOF文件的大小一般会比RDB文件大。
在某些fsync策略下，AOF的速度会比RDB慢。通常fsync设置为每秒一次就能获得比较高的性能，而在禁止fsync的情况下速度可以达到RDB的水平。

``` shell
##此选项为aof功能的开关，默认为“no”，可以通过“yes”来开启aof功能  
##只有在“yes”下，aof重写/文件同步等特性才会生效  
appendonly yes  

##指定aof文件名称  
appendfilename appendonly.aof  

##指定aof操作中文件同步策略，有三个合法值：always everysec no,默认为everysec  
appendfsync everysec  
##在aof-rewrite期间，appendfsync是否暂缓文件同步，"no"表示“不暂缓”，“yes”表示“暂缓”，默认为“no”  
no-appendfsync-on-rewrite no  

##aof文件rewrite触发的最小文件尺寸(mb,gb),只有大于此aof文件大于此尺寸是才会触发rewrite，默认“64mb”，建议“512mb”  
auto-aof-rewrite-min-size 64mb  

##相对于“上一次”rewrite，本次rewrite触发时aof文件应该增长的百分比。  
##每一次rewrite之后，redis都会记录下此时“新aof”文件的大小(例如A)，那么当aof文件增长到A*(1 + p)之后  
##触发下一次rewrite，每一次aof记录的添加，都会检测当前aof文件的尺寸。  
auto-aof-rewrite-percentage 100  
```
AOF是文件操作，对于变更操作比较密集的server，那么必将造成磁盘IO的负荷加重；此外linux对文件操作采取了“延迟写入”手段，即并非每次write操作都会触发实际磁盘操作，而是进入了buffer中，当buffer数据达到阀值时触发实际写入(也有其他时机)，这是linux对文件系统的优化，但是这却有可能带来隐患，如果buffer没有刷新到磁盘，此时物理机器失效(比如断电)，那么有可能导致最后一条或者多条aof记录的丢失。通过上述配置文件，可以得知redis提供了3种aof记录同步选项：

always：每一条aof记录都立即同步到文件，这是最安全的方式，也以为更多的磁盘操作和阻塞延迟，是IO开支较大。
everysec：每秒同步一次，性能和安全都比较中庸的方式，也是redis推荐的方式。如果遇到物理服务器故障，有可能导致最近一秒内aof记录丢失(可能为部分丢失)。
no：redis并不直接调用文件同步，而是交给操作系统来处理，操作系统可以根据buffer填充情况/通道空闲时间等择机触发同步；这是一种普通的文件操作方式。性能较好，在物理服务器故障时，数据丢失量会因OS配置有关。
其实，我们可以选择的太少，everysec是最佳的选择。如果你非常在意每个数据都极其可靠，建议你选择一款“关系性数据库”吧。 

## 主从

通过持久化功能，Redis保证了即使在服务器重启的情况下也不会损失（或少量损失）数据，因为持久化会把内存中数据保存到硬盘上，重启会从硬盘上加载数据。 
。但是由于数据是存储在一台服务器上的，如果这台服务器出现硬盘故障等问题，也会导致数据丢失。为了避免单点故障，通常的做法是将数据库复制多个副本以部署在不同的服务器上，这样即使有一台服务器出现故障，其他服务器依然可以继续提供服务。为此， Redis 提供了复制（replication）功能，可以实现当一台数据库中的数据更新后，自动将更新的数据同步到其他数据库上。
1、redis的复制功能是支持多个数据库之间的数据同步。一类是主数据库（master）一类是从数据库（slave），主数据库可以进行读写操作，当发生写操作的时候自动将数据同步到从数据库，而从数据库一般是只读的，并接收主数据库同步过来的数据，一个主数据库可以有多个从数据库，而一个从数据库只能有一个主数据库。

2、通过redis的复制功能可以很好的实现数据库的读写分离，提高服务器的负载能力。主数据库主要进行写操作，而从数据库负责读操作

### 过程
1：当一个从数据库启动时，会向主数据库发送sync命令，

2：主数据库接收到sync命令后会开始在后台保存快照（执行rdb操作），并将保存期间接收到的命令缓存起来

3：当快照完成后，redis会将快照文件和所有缓存的命令发送给从数据库。

4：从数据库收到后，会载入快照文件并执行收到的缓存的命令。

主从复制是乐观复制，当客户端发送写执行给主，主执行完立即将结果返回客户端，并异步的把命令发送给从，从而不影响性能。也可以设置至少同步给多少个从主才可写。 
无硬盘复制:如果硬盘效率低将会影响复制性能，2.8之后可以设置无硬盘复制，repl-diskless-sync yes



## sentinel作用
A、Master 状态监测

B、如果Master 异常，则会进行Master-slave 转换，将其中一个Slave作为Master，将之前的Master作为Slave 

C、Master-Slave切换后，master_redis.conf、slave_redis.conf和sentinel.conf的内容都会发生改变，即master_redis.conf中会多一行slaveof的配置，sentinel.conf的监控目标会随之调换 

哨兵就是为了监测你的主数据库是否出问题，如果主数据库出问题就会把从数据库升为主数据库，也可以设置多个哨兵来监测主从，然后多个哨兵监测的时候就会运用投票来监测主从，
如果是主数据库挂掉 ，就需要你的这些多个哨兵来投票 当多票当选的时候就会让从数据库变成主，当有哨兵机制的时候是进行连接哨兵 ，哨兵再连接数据库
# redis lua

``` scilab
eval "return redis.call('set','foo','bar')" 0
eval "return redis.call('set',KEYS[1],'bar')" 1 foo
```

# 分布式锁

``` processing
public boolean lock() {
        // 请求锁超时时间，纳秒
        long timeout = timeOut * 100000000;
        // 系统当前时间，纳秒
        long nowTime = System.nanoTime();

        while ((System.nanoTime() - nowTime) < timeout) {
            // 分布式服务器有时差，这里给1秒的误差值
            expires = System.currentTimeMillis() + expireTime + 1;
            String expiresStr = String.valueOf(expires); //锁到期时间

            if (redisTemplate.opsForValue().setIfAbsent(lockKey, expiresStr)) {
                locked = true;
                // 设置锁的有效期，也是锁的自动释放时间，也是一个客户端在其他客户端能抢占锁之前可以执行任务的时间
                // 可以防止因异常情况无法释放锁而造成死锁情况的发生
                redisTemplate.expire(lockKey, expireTime, TimeUnit.SECONDS);

                // 上锁成功结束请求
                return true;
            }

            String currentValueStr = redisTemplate.opsForValue().get(lockKey); //redis里的时间
            if (currentValueStr != null && Long.parseLong(currentValueStr) < System.currentTimeMillis()) {
                //判断是否为空，不为空的情况下，如果被其他线程设置了值，则第二个条件判断是过不去的
                // lock is expired

                String oldValueStr = redisTemplate.opsForValue().getAndSet(lockKey, expiresStr);
                //获取上一个锁到期时间，并设置现在的锁到期时间，
                //只有一个线程才能获取上一个线上的设置时间，因为jedis.getSet是同步的
                if (oldValueStr != null && oldValueStr.equals(currentValueStr)) {
                    //防止误删（覆盖，因为key是相同的）了他人的锁——这里达不到效果，这里值会被覆盖，但是因为什么相差了很少的时间，所以可以接受

                    //[分布式的情况下]:如过这个时候，多个线程恰好都到了这里，但是只有一个线程的设置值和当前值相同，他才有权利获取锁
                    // lock acquired
                    locked = true;
                    return true;
                }
            }
            /*
                延迟10 毫秒,  这里使用随机时间可能会好一点,可以防止饥饿进程的出现,即,当同时到达多个进程,
                只会有一个进程获得锁,其他的都用同样的频率进行尝试,后面有来了一些进行,也以同样的频率申请锁,这将可能导致前面来的锁得不到满足.
                使用随机的等待时间可以一定程度上保证公平性
             */
            try {
                Thread.sleep(100, random.nextInt(50000));
            } catch (InterruptedException e) {
                logger.error("获取分布式锁休眠被中断：", e);
            }

        }
        return locked;
    }


    /**
     * 解锁
     */
    public synchronized void unlock() {
        // 只有加锁成功并且锁还有效才去释放锁
        if (locked && expires > System.currentTimeMillis()) {
            redisTemplate.delete(lockKey);
            locked = false;
        }
    }
```
# pipeline 

``` processing
public Map<String, Boolean> scriptBfContains(List<String> keyList, List<String> valueList) {
        List<Object> result = new ArrayList<>();
        redisTemplate.executePipelined(new RedisCallback<Long>() {
            @Nullable
            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                if (!CollectionUtils.isEmpty(keyList)) {
                    for (String key : keyList) {
                        connection.scriptingCommands().eval(joinBfCommand("bf.mexists", key, valueList).getBytes(Charset.forName("UTF-8")), ReturnType.MULTI, 0);
                    }
                }
                result.addAll(connection.closePipeline());
                return null;
            }
        }, redisTemplate.getValueSerializer());
        Map<String, Boolean> hashMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(result)) {
            hashMap = valueList.stream().collect(Collectors.toMap(key -> key, key -> (((ArrayList) result.get(0)).get(valueList.indexOf(key)).toString()).equals("1") ? true : false));
        }

        return hashMap;
    }
```
# lambda

``` lasso
valueList.stream().collect(Collectors.toMap(key -> key, key -> (((ArrayList) result.get(0)).get(valueList.indexOf(key)).toString()).equals("1") ? true : false));
// 有分片数据
Map<String, Boolean> inexistenceMap = map.entrySet().stream().filter(v -> v.getValue().equals(false))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
List<String> newIds = ids.stream().map(e -> new StringBuilder().append(deviceId).append("_").append(e).toString()).collect(Collectors.toList());
resultMap.entrySet().stream().collect(Collectors.toMap(
                    k -> k.getKey().substring(k.getKey().lastIndexOf("_") + 1),
                    Map.Entry::getValue
            ))
			
```

# 布隆过滤器
## redission

``` 
@Configuration
public class RedissonConfig  {

    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private String port;


    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress(String.format("redis://%s:%s",host,port));
        return Redisson.create(config);
    }
}

    @Autowired
    RedissonClient redissonClient;

    public void redissonBfAdd(String key, List<String> valueList) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(key);
        bloomFilter.tryInit(bloomFilterConfig.getSize(), bloomFilterConfig.getFpp());
        for (String value : valueList) {
            bloomFilter.add(value); // bloomFilter.contains(value);
        }

    }
	
```
我们可以把它看做一个会产生误判并且占用空间极少的HashSet。它的结构是一个Bit数组（数组中每个位置只占用一个bit，每个bit位有0和1两种状态）和一系列Hash函数的集合，我们将输入域通过上述一系列Hash函数进行Hash运算得到n个key值，将这n个值对数组的长度进行取余，然后将bit数组中对应的位置bit位设为1。在数组足够大，hash碰撞足够小的情况下，每个输入域都会在数组中不同的位置将其bit位置为1，我们把集合中所有的元素都按照这个方式来一遍的话一个布隆过滤器就生成好了。

那么如何判断一个元素是否在布隆过滤器中呢，原理和生成布隆过滤器的过程差不多，我们将要判断的值通过布隆过滤器的n个Hash函数计算出n个值，对数组长度取余得到bit数组中n个位置，接下来判断这n个位置的bit位是否都为1，若都为1，则说明该元素在集合中，若有一个为0，则该元素肯定不在集合中。
计算布隆过滤器的空间占用：m=-n*lnp/(lnx)^2 其中n为元素的个数，p为允许的误差率大小
布隆过滤器的hash函数的个数：k=(ln2)*m/n

已经知晓布隆过滤器的作用是检索一个元素是否在集合中。可能有人认为这个功能非常简单，直接放在redis中或者数据库中查询就好了。又或者当数据量较小，内存又足够大时，使用hashMap或者hashSet等结构就好了。但是如果当这些数据量很大，数十亿甚至更多，内存装不下且数据库检索又极慢的情况，我们应该如何去处理？这个时候我们不妨考虑下布隆过滤器，因为它是一个空间效率占用极少和查询时间极快的算法，但是需要业务可以忍受一个判断失误率。
## lua
bf.add codehole user1
bf.exists codehole user1
bf.madd codehole user4 user5 user6
bf.mexists codehole user4 user5 user6 user7
bf.reserve  codehole  0.01 40000000