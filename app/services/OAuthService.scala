package services

import play.api.libs.concurrent.Promise

/**
 * @author Ryan Brainard
 */

trait OAuthService {
  def userAuthUrl(callbackHost: String): String
}

trait OAuth1Service extends OAuthService {
  def retrieveAccessToken(oauthToken: String, oauthVerifier: String): Promise[String]
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
