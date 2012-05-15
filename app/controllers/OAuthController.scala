package controllers

import play.api.mvc._
import services.{OAuth2Service, OAuth1Service, OAuthService}

object OAuthController extends Controller {

  case class OAuthAccess[S <: OAuthService](token: String)
  
  def using[A, S <: OAuthService](service: S)(innerAction: OAuthAccess[S] => Request[A] => Result)
                                  (implicit p: BodyParser[A] = parse.anyContent) = Action(p) {
    request =>
      request.session.get(oauthAccessTokenKey(service)).map {
        accessToken =>
          innerAction(OAuthAccess(accessToken))(request)
      }.getOrElse(Redirect(service.userAuthUrl))
  }

  def key(serviceName: String): Action[AnyContent] = OAuthService(serviceName).map {
    service =>
      using(service) {
        implicit oauthAccess =>
          implicit request2: Request[AnyContent] =>
            Ok(oauthAccess.token)
      }
  }.getOrElse(Action(NotFound("Unknown OAuth Service")))

  def handleCallbackOAuth1(serviceName: String, oauth_token: String, oauth_verifier: String) = Action {
    implicit request =>
      OAuthService(serviceName) match {
        case Some(service: OAuth1Service) =>
          handleCallbackSuccessOAuth1(service, oauth_token, oauth_verifier)
        case None => InternalServerError("Unknown OAuthController Service")
      }
  }
  
  def handleCallbackOAuth2(serviceName: String, code: String, error: String) = Action {
    implicit request =>
      OAuthService(serviceName) match {
        case Some(service: OAuth2Service) =>
          if (error != null) { handleCallbackError(service, error) }
          else if (code != null) { handleCallbackSuccessOAuth2(service, code) }
          else { InternalServerError("Unknown OAuthController State") }
        case None => InternalServerError("Unknown OAuthController Service")
      }
  }

  private def handleCallbackSuccessOAuth1(service: OAuth1Service, oauthToken: String, oauthVerifier: String)(implicit request: Request[AnyContent]) = {
    Async {
      service.retrieveAccessToken(oauthToken, oauthVerifier).map {
        accessToken =>
          Redirect("/").withSession(
            session + (oauthAccessTokenKey(service) -> accessToken)
          )
      }
    }
  }

  private def handleCallbackSuccessOAuth2(service: OAuth2Service, code: String)(implicit request: Request[AnyContent]) = {
      Async {
        service.retrieveAccessToken(code).map {
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