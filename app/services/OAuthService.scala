package services

import play.api.libs.concurrent.Promise
import oauth.signpost.basic.DefaultOAuthConsumer
import oauth.signpost.OAuthProvider

/**
 * @author Ryan Brainard
 */

trait OAuthService {
  val name: String
  def userAuthUrl(callbackHost: String): String
}

trait OAuth1Service extends OAuthService {
  val consumerKey: String
  val consumerSecret: String
  val provider: OAuthProvider

  def userAuthUrl(callbackHost: String) = {
    val consumer: DefaultOAuthConsumer = newMutableConsumer()
    val authUrl: String = provider.retrieveRequestToken(consumer, "http://" + callbackHost + "/oauth1/callback/" + name)
    Redis.exec(_.setex(oauthTokenSecretKey(consumer.getToken), 5 * 60, consumer.getTokenSecret))
    authUrl
  }

  def retrieveAccessToken(oauthToken: String, oauthVerifier: String): Promise[String] = {
    val consumer = newMutableConsumer()
    consumer.setTokenWithSecret(oauthToken, Redis.exec(_.get(oauthTokenSecretKey(oauthToken))))
    provider.retrieveAccessToken(consumer, oauthVerifier)
    Promise.pure(consumer.getToken)
  }

  private def newMutableConsumer(): DefaultOAuthConsumer = new DefaultOAuthConsumer(consumerKey, consumerSecret)

  private def oauthTokenSecretKey(token: String) = "OAUTH_TOKEN_SECRET" + this.name + token
}

trait OAuth2Service extends OAuthService {
  def retrieveAccessToken(code: String): Promise[String]
}

object OAuthService {
  def apply(serviceName: String): Option[OAuthService] = {
    serviceName match {
      case "github" => Some(GitHubApi)
      case "trello" => Some(TrelloApi)
      case _ => None
    }
  }
}
