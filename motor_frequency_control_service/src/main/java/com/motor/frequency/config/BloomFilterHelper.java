package com.motor.frequency.config;

import com.google.common.base.Preconditions;
import com.google.common.hash.Funnel;
import com.google.common.hash.Hashing;

/**
 * @program: motor_frequency_control
 * @description: 布隆过滤器
 * @author: OneJane
 * @create: 2020-03-16 13:56
 **/
public class BloomFilterHelper<T> {
	private int numHashFunctions;

	private int bitSize;

	private Funnel<T> funnel;

	/**
	 * 布隆过滤器大小，m=-(n*lnp)/(ln2)^2
	 * 哈希函数个数，k=ln2*(m/n)
	 * 真实失误率p，(1-e^(-n*k/m))^k
	 * @param funnel 将数据发送给一个接收器
	 * @param expectedInsertions 预估的元素个数
	 * @param fpp  错误率
	 */
	public BloomFilterHelper(Funnel<T> funnel, int expectedInsertions, double fpp) {
		Preconditions.checkArgument(funnel != null, "funnel不能为空");
		this.funnel = funnel;
		bitSize = optimalNumOfBits(expectedInsertions, fpp);
		numHashFunctions = optimalNumOfHashFunctions(expectedInsertions, bitSize);

	}

	public long[] murmurHashOffset(T value) {
		long[] offset = new long[numHashFunctions];
		long hash64 = Hashing.murmur3_128().hashObject(value, funnel).asLong();
		int hash1 = (int) hash64;
		int hash2 = (int) (hash64 >>> 32);
		for (int i = 1; i <= numHashFunctions; i++) {
			int nextHash = hash1 + i * hash2;
			if (nextHash < 0) {
				nextHash = ~nextHash;
			}
			offset[i - 1] = nextHash % bitSize;
		}

		return offset;
	}

	/**
	 * 计算bit数组的长度
	 */
	private int optimalNumOfBits(long n, double p) {
		if (p == 0) {
			p = Double.MIN_VALUE;
		}
		return (int) (-n * Math.log(p) / (Math.log(2) * Math.log(2)));
	}

	/**
	 * 计算hash方法执行次数
	 */
	private int optimalNumOfHashFunctions(long n, long m) {
		return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
	}
}
