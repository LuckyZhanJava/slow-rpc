# slow rpc：一个简单的rpc实现

## 1.简单实现
总的来说一个rpc实现和kafka的api实现类似，为每个调用包含一个唯一`id`，然后序列化调用参数为`byte[]`，把`id`和`byte[]`组成通用`request`，服务端接收`request`后解析`id`和参数`byte[]`，根据id查找方法，然后调用方法，最后序列化返回值后转为`response`写回客户端。

在这个基础上做一些改动就可以实现简单的rpc框架。
1. 首先要用到`Proxy.newProxyInstance(...)`为接口生成代理，在`InvocationHandler`中可以拿到调用的`method`和`args`。
2. 为`method`生成唯一id供服务端匹配，使用:`interfaceName + methodName + parameterTypes`可以确保id唯一。
3. 序列化`method`的`id`和`args`组成`request`发送请求。
4. 服务端解析`id`查找`method`，使用`args`调用方法，产生返回值，以`response`的形式写回。
5. 客户端读取`response`并解析返回值，返回给方法调用即可。

这里在请求和响应部分做了改动。

使用已有的`HttpClient`库可以轻易实现`http`协议的`requst`和`response`，但是支持其他类似`Dubbo`的协议并不容易。

使用`netty`可以轻松实现新的协议，可以采用类似`HttpClient`的实现，每次请求创建或者从连接池中取出一个`channel`，然后执行写入`request`,一直等到`channel`读取到一个完整的响应关闭或者释放`channel`,这样就产生了一个问题，慢连接或者慢方法调用会导致`channel`频繁创建。

所以这里采用多个调用复用单个`channel`的实现，因为`channel.write`总是在`eventloop`中线性的执行，多个`request`对象被一个挨一个的写出，然后服务端处理`request`再把`response`一个一个的写回即可。在服务端串行调用方法的效率不高，如果改为线程池并发调用则`response`的写回顺序可能和`request`的写出顺序不同，为了匹配`request`和`response`，这里对`request`进行唯一`seqNumber`标识，`response`写回时携带`seqNumber`即可。

具体实现如下：

1. 每个`request`对象都包含一个负责保存响应值的`CompletableFuture`对象，每次写出`request`,以`seqNumber`为key保存`request`到`Map<Long,Request> map`中。
2. `client.write(request)`调用`channel.write(request)`，然后返回`request`包含的`CompletableFuture`对象。
3. `channel.read(...)`读取到`response`对象时，根据`response`的`seqNumber`取出匹配的`request`对象，然后填充`CompletableFuture`即可。


## 2.负载均衡，熔断等等支持

这部分的实现主要是依赖`Eureka`等已有的实现。

整体的实现类似`FilterChain`，在调用链的末端是实际调用，在调用过程中，可以修改向下传递的参数来控制最后的调用，也可以直接返回结果中断后续的调用。前者通过修改向下传递的`server`来实现负载均衡，后者则可以在出现熔断时中止调用直接返回错误。
