package controllers

import play.api.mvc._
import services.OAuthService

object OAuthController extends Controller {

  case class OAuthAccess[S <: OAuthService](token: String)
  
  def using[A, S <: OAuthService](service: S)(innerAction: OAuthAccess[S] => Request[A] => Result)
                                  (implicit p: BodyParser[A]) = Action(p) {
    request =>
      request.session.get(oauthAccessTokenKey(service)).map {
        accessToken =>
          innerAction(OAuthAccess(accessToken))(request)
      }.getOrElse(Redirect(service.userAuthUrl))
  }

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