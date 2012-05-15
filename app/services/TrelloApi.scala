package services

import play.api.libs.ws.WS
import play.api.libs.concurrent.Promise
import collection.Seq
import play.api.Logger
import com.codahale.jerkson.{ParsingException, Json}
import controllers.OAuthController.OAuthAccess
import models.Trello.Board
import oauth.signpost.basic.DefaultOAuthProvider
import oauth.signpost.OAuthProvider

/**
 * @author Ryan Brainard
 */

object TrelloApi extends OAuth1Service {

  val name = "trello"
  val consumerKey = sys.env.getOrElse("TRELLO_CONSUMER_KEY", sys.error("TRELLO_CONSUMER_KEY not configured"))
  val consumerSecret = sys.env.getOrElse("TRELLO_CONSUMER_SECRET", sys.error("TRELLO_CONSUMER_SECRET not configured"))
  val requestTokenEndpointUrl = "https://trello.com/1/OAuthGetRequestToken"
  val accessTokenEndpointUrl = "https://trello.com/1/OAuthGetAccessToken"
  val authorizationWebsiteUrl = "https://trello.com/1/OAuthAuthorizeToken"
  val provider: OAuthProvider = new DefaultOAuthProvider(requestTokenEndpointUrl, accessTokenEndpointUrl, authorizationWebsiteUrl)

  def apply()(implicit access: OAuthAccess[TrelloApi.type]) = new TrelloApi(access)
}

class TrelloApi(access: OAuthAccess[TrelloApi.type]) {

  private val baseApiUrl = "https://api.trello.com/1"

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

  def getBoards(): Promise[Seq[Board]] = get[Seq[Board]]("/members/me/boards")
}
