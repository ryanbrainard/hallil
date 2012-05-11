package services

import java.net.URI
import redis.clients.jedis.{Jedis, JedisPoolConfig, JedisPool}

/**
 * @author Ryan Brainard
 */

object Redis {

  private val redisUrl = new URI(sys.env.getOrElse("REDISTOGO_URL", sys.error("REDISTOGO_URL not configured")))
  private val redisPassword: Option[String] = Option(redisUrl.getUserInfo).map(_.split(":").apply(1))
  private val redisPool = new JedisPool(new JedisPoolConfig(), redisUrl.getHost, redisUrl.getPort, 2000, redisPassword.getOrElse(null))

  def exec[T](body: Jedis => T): T = {
    val j = redisPool.getResource
    try {
      body(j)
    } finally {
      redisPool.returnResource(j)
    }
  }
}
