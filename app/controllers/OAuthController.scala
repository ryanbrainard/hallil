package controllers

import play.api.mvc._
import play.api.libs.concurrent.Promise
import services.{OAuth2Service, OAuth1Service, OAuthService}

object OAuthController extends Controller {

  case class OAuthAccess[S <: OAuthService](token: String)

  def using[A, S <: OAuthService](service: S)(innerAction: OAuthAccess[S] => Request[A] => Result)
                                 (implicit p: BodyParser[A] = parse.anyContent) = Action(p) {
    implicit request =>
      request.session.get(oauthAccessTokenKey(service)).map {
        accessToken =>
          innerAction(OAuthAccess(accessToken))(request)
      }.getOrElse {
        val callbackHost: String = request.headers("Host")
        Redirect(service.userAuthUrl(callbackHost)).withSession {
          session + ("oauthRetUrl" -> request.uri)
        }
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
          handleCallbackSuccess(service, service.retrieveAccessToken(oauth_token, oauth_verifier))
        case None => InternalServerError("Unknown OAuth 1.0 Service")
      }
  }

  def handleCallbackOAuth2(serviceName: String, code: String, error: String) = Action {
    implicit request =>
      OAuthService(serviceName) match {
        case Some(service: OAuth2Service) =>
          if (error != null) { InternalServerError("OAuth 2.0 Error: " + error) }
          else if (code != null) { handleCallbackSuccess(service, service.retrieveAccessToken(code)) }
          else { InternalServerError("Unknown OAuth 2.0 State") }
        case None => InternalServerError("Unknown OAuth 2.0 Service")
      }
  }

  private def handleCallbackSuccess(service: OAuthService, retrieveAccessToken: => Promise[String])
                                        (implicit request: Request[AnyContent]) = Async {
    retrieveAccessToken.map {
      accessToken =>
        Redirect(session.get("oauthRetUrl").getOrElse("/")).withSession(
          session - ("oauthRetUrl")
                  + (oauthAccessTokenKey(service) -> accessToken)
        )
    }
  }

  private def oauthAccessTokenKey(service: OAuthService): String = {
    "OAUTH_ACCESS_TOKEN_" + service.name
  }
}