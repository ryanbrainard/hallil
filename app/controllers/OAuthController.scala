package controllers

import play.api.mvc._
import services.{OAuth2Service, OAuth1Service, OAuthService}
import play.api.libs.concurrent.Promise

object OAuthController extends Controller {

  case class OAuthAccess[S <: OAuthService](token: String)

  def using[A, S <: OAuthService](service: S)(innerAction: OAuthAccess[S] => Request[A] => Result)
                                 (implicit p: BodyParser[A] = parse.anyContent) = Action(p) {
    request =>
      request.session.get(oauthAccessTokenKey(service)).map {
        accessToken =>
          innerAction(OAuthAccess(accessToken))(request)
      }.getOrElse {
        val callbackHost: String = request.headers("Host")
        Redirect(service.userAuthUrl(callbackHost))
      }
  }

  def key(serviceName: String): Action[AnyContent] = OAuthService(serviceName).map { service =>
    using(service) {
      implicit oauthAccess =>
        implicit request: Request[AnyContent] =>
          Ok(oauthAccess.token)
    }
  }.getOrElse(Action(NotFound("Unknown OAuth Service")))

  def handleCallbackOAuth1(serviceName: String, oauth_token: String, oauth_verifier: String) = Action {
    implicit request =>
      OAuthService(serviceName) match {
        case Some(service: OAuth1Service) =>
          handleCallbackSuccessOAuth(service, service.retrieveAccessToken(oauth_token, oauth_verifier))
        case None => InternalServerError("Unknown OAuth 1.0 Service")
      }
  }

  def handleCallbackOAuth2(serviceName: String, code: String, error: String) = Action {
    implicit request =>
      OAuthService(serviceName) match {
        case Some(service: OAuth2Service) =>
          if (error != null) { InternalServerError("OAuth 2.0 Error: " + error) }
          else if (code != null) { handleCallbackSuccessOAuth(service, service.retrieveAccessToken(code)) }
          else { InternalServerError("Unknown OAuth 2.0 State") }
        case None => InternalServerError("Unknown OAuth 2.0 Service")
      }
  }

  private def handleCallbackSuccessOAuth(service: OAuthService, retrieveAccessToken: => Promise[String])
                                        (implicit request: Request[AnyContent]) = {
    Async {
      retrieveAccessToken.map {
        accessToken =>
          Redirect("/").withSession(
            session + (oauthAccessTokenKey(service) -> accessToken)
          )
      }
    }
  }

  private def oauthAccessTokenKey(service: OAuthService): String = {
    "OAUTH_ACCESS_TOKEN_" + service.name
  }
}