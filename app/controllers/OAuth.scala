package controllers

import play.api.mvc._
import play.api.mvc.Security._
import services.{OAuthService, GitHub}

object OAuth extends Controller {

  /**
   * Action wrapper for
   *
   * @param service OAuth service to use to authenticate
   * @param action action to wrap
   * @tparam A the type of the request body
   * @return
   */
  def using[A](service: OAuthService)(action: String => Action[A]): Action[(Action[A], A)] = Authenticated(
    requestHeader => requestHeader.session.get(oauthAccessTokenKey(service)),
    _ => Redirect(service.userAuthUrl))(action)

  def callback(service: String,  code: String) = {
    handleCallback(service match {
      case "github" => GitHub
    })(code)
  }

  private def handleCallback(service: OAuthService)(code: String) = Action {
    implicit request =>
      Async {
        service.exchangeCodeForAccessToken(code).map {
          accessToken =>
            Redirect("/").withSession(
              session + (oauthAccessTokenKey(service) -> accessToken)
            )
        }
      }
  }

  private def oauthAccessTokenKey(service: OAuthService): String = {
    "OAUTH_ACCESS_TOKEN_" + service.getClass.getName
  }
}