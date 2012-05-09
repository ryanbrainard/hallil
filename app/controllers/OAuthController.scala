package controllers

import play.api.mvc._
import play.api.mvc.Security._
import services.OAuthService

object OAuthController extends Controller {

  /**
   * Action wrapper for authenticating with OAuth
   *
   * @param service OAuthController service to use to authenticate
   * @param action action to wrap
   * @tparam A the type of the request body
   * @return
   */
  def using[A](service: OAuthService)(action: String => Action[A]): Action[(Action[A], A)] = Authenticated(
    requestHeader => requestHeader.session.get(oauthAccessTokenKey(service)),
    _ => Redirect(service.userAuthUrl))(action)


  def handleCallback(serviceName: String, code: String, error: String) = Action {
    implicit request =>
      OAuthService(serviceName) match {
        case Some(service) => 
          if (error != null) { handleCallbackError(service, error) }
          else if (code != null) { handleCallbackSuccess(service, code) }
          else { InternalServerError("Unknown OAuthController State") }
        case None => InternalServerError("Unknown OAuthController Service")
      }
  }
  
  private def handleCallbackSuccess(service: OAuthService, code: String)(implicit request: Request[AnyContent]) = {
      Async {
        service.exchangeCodeForAccessToken(code).map {
          accessToken =>
            Redirect("/").withSession(
              session + (oauthAccessTokenKey(service) -> accessToken)
            )
        }
      }
  }
  
  private def handleCallbackError(service: OAuthService, error: String) = {
    InternalServerError("OAuthController Error: " + error)
  }

  private def oauthAccessTokenKey(service: OAuthService): String = {
    "OAUTH_ACCESS_TOKEN_" + service.getClass.getName
  }
}