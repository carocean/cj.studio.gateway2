package cj.studio.gateway.socket.cable;

import cj.studio.ecm.graph.CircuitException;
import cj.ultimate.IClosable;

/**
 * 电缆
 * 
 * <pre>
 * - 电缆是导线池，导线是连接。
 * - 调用方用法：
 * 	- 从电缆中获取空闲的导线，如果无空闲导线，则等待，直到超时；
 * 	- 用完一次后即关闭导线，关闭导线并非是关闭物理连结，而是将导线设为空闲，交由电缆来管理
 * - 相关属性：
 * 	- 最大连接数：不能再申请新连接，如果导线均忙，则等待，直到超时，超时时间默认是15s
 * 	- 最小空闲数：当电缆启动即按此数建立导线并与远程连结；当池中的空闲导线超过此数，则释放一些导线（关闭物理连结并从电缆中移除导线）
 * 	- 当前连接数：当前电缆中的导线数目。
 * 	- 当前活动数：当前电览中处于“忙”的导线数目。忙表示为调用者还未调用导线的close方法
 * 	- 当前空闲数：当前电览中徙理“闲”的导线数目。
 * 	- 在任何随机时间点上：当前连接数=当前活动数+当前空闲数，要通过同步方案保证线程安全。
 * - 导线物理连结断开时自动从电缆中移除
 * </pre>
 * 
 * @author caroceanjofers
 *
 */
public interface IGatewaySocketCable extends IClosable {
	/**
	 * 定义在连接远程获取新连接失败后重复尝试的次数。Default: 3
	 * 
	 * @return
	 */
	int acquireRetryAttempts();


	/**
	 * 最大空闲时间,n毫秒内未使用则导线被丢弃。若为0则永不丢弃。Default: 0
	 * 
	 * @return
	 */
	long maxIdleTime();

	/**
	 * 电缆中保留的最大导线数
	 * 
	 * @return
	 */
	int maxWireSize();

	/**
	 * 电缆中保留的最小导线数
	 * 
	 * @return
	 */
	int minWireSize();
	/**
	 * 初始化时建立minWireSize个导线，取值应在minWireSize与maxWireSize之间。Default: minWireSize
	 * @return
	 */
	int initialWireSize();
	/**
	 * 当导线用完时调用select()后等待获取新连接的时间，超时后将抛出导常
	 * ,如设为0则无限期等待。单位毫秒。Default: 0
	 * 
	 * @return
	 */
	long checkoutTimeout();
	/**
	 * 当导线发送请求后等待响应的时间，超时后将抛出导常
	 * ,如设为0则无限期等待。单位毫秒。Default: 0
	 * 
	 * @return
	 */
	long requestTimeout();
	/**
	 * 聚合器限制聚合的最大大小
	 * ,单位字节。Default: 3m
	 * 
	 * @return
	 */
	int aggregatorLimit();
	/**
	 * 尝试获取一个可用导线。如果池满且无空闲导线将等待，直到超时。该方法在导线数没达到上限时会新建导线。
	 * 
	 * @return
	 */
	IGatewaySocketWire select() throws CircuitException;
	/**
	 * 初始化并解析字符
	 * @param connStr 连接关描述符，格式：tcp://ip:port?minWireSize=3&maxWireSize=10&checkoutTimeout=30
	 * @throws CircuitException
	 */
	void init(String connStr) throws CircuitException;
	/**
	 * 启动并建立连接
	 * @param connStr 连接关描述符，格式：tcp://ip:port?minWireSize=3&maxWireSize=10&checkoutTimeout=30
	 * @throws CircuitException
	 */
	void connect() throws CircuitException;


	String host();


	int port();


	String protocol();
}
