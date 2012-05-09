package services

import play.api.libs.concurrent.Promise

/**
 * @author Ryan Brainard
 */

trait OAuthService {
  def userAuthUrl: String
  def exchangeCodeForAccessToken(code: String): Promise[String]
}

object OAuthService {
  def apply(serviceName: String): Option[OAuthService] = {
    serviceName match {
      case "github" => Some(GitHubApi)
      case _ => None
    }
  }
}
