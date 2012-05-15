package services

import play.api.libs.ws.WS
import play.api.libs.concurrent.Promise
import models._
import collection.Seq
import play.api.Logger
import com.codahale.jerkson.{ParsingException, Json}
import controllers.OAuthController.OAuthAccess
import oauth.signpost.basic.{DefaultOAuthProvider, DefaultOAuthConsumer}
import oauth.signpost.{OAuth, OAuthConsumer, OAuthProvider}
import models.Trello.Board

/**
 * @author Ryan Brainard
 */

object TrelloApi extends OAuth1Service {

  private val consumerKey = sys.env.getOrElse("TRELLO_CONSUMER_KEY", sys.error("TRELLO_CONSUMER_KEY not configured"))
  private val consumerSecret = sys.env.getOrElse("TRELLO_CONSUMER_SECRET", sys.error("TRELLO_CONSUMER_SECRET not configured"))

  private def newConsumer(): DefaultOAuthConsumer = new DefaultOAuthConsumer(consumerKey, consumerSecret)
  private val provider: DefaultOAuthProvider = new DefaultOAuthProvider("https://trello.com/1/OAuthGetRequestToken",
                                                                        "https://trello.com/1/OAuthGetAccessToken",
                                                                        "https://trello.com/1/OAuthAuthorizeToken")
  def userAuthUrl = {
    val consumer: DefaultOAuthConsumer = newConsumer()
    val authUrl: String = provider.retrieveRequestToken(consumer, "http://localhost:9000/oauth1/callback/trello")
    Redis.exec(_.setex(consumer.getToken, 5 * 60, consumer.getTokenSecret))
    authUrl
  }

  def retrieveAccessToken(oauthToken: String, oauthVerifier: String): Promise[String] = {
    val consumer = newConsumer()
    consumer.setTokenWithSecret(oauthToken, Redis.exec(_.get(oauthToken)))
    provider.retrieveAccessToken(consumer, oauthVerifier)
    Promise.pure(consumer.getToken())
  }

  def apply()(implicit access: OAuthAccess[TrelloApi.type]) = new TrelloApi(access)
}

class TrelloApi(access: OAuthAccess[TrelloApi.type]) {

  private val baseApiUrl = "https://api.trello.com"

  private def get[A](url: String)(implicit mf: Manifest[A]) = {
    val fullUrl = if (url.startsWith("http")) url else baseApiUrl + url
    Logger.debug("GET " + fullUrl)

    WS.url(fullUrl).withQueryString("key" -> TrelloApi.consumerKey, "token" -> access.token).get().map {
      response =>
        try {
          Json.parse[A](response.body)(mf)
        } catch {
          case e: ParsingException =>
            Logger.error("Failed to parse response from " + fullUrl + "\n" + response.body, e)
            throw e
        }
    }
  }

  def getBoards(): Promise[Seq[Board]] = get[Seq[Board]]("/1/members/me/boards")
}
