package controllers

import play.api.libs.ws.WS
import play.api.mvc.{Action, Controller}
import play.api.Play

/**
 * @author Ryan Brainard
 */

object Github extends Controller {

  private val ClientId = sys.env.getOrElse("GITHUB_CLIENT_ID", sys.error("GITHUB_CLIENT_ID not configured"))
  private val ClientSecret = sys.env.getOrElse("GITHUB_CLIENT_SECRET", sys.error("GITHUB_CLIENT_SECRET not configured"))
  private val UserAuthUrl = "https://github.com/login/oauth/authorize?client_id=%s".format(ClientId)
  private def AccessTokenExchangeUrl(code: String) = "https://github.com/login/oauth/access_token?client_id=%s&client_secret=%s&code=%s".format(ClientId, ClientSecret, code)
  private val AccessTokenExchangeResponsePattern = """.*access_token=(\w+).*""".r
  private val GithubOAuthAccessTokenSessionKey: String = "GITHUB_OAUTH_ACCESS_TOKEN"

  def Authenticated[A](action: String => Action[A]): Action[(Action[A], A)] = play.api.mvc.Security.Authenticated(
    requestHeader => {println("01:"+ requestHeader.session.get(GithubOAuthAccessTokenSessionKey));requestHeader.session.get(GithubOAuthAccessTokenSessionKey)},
    _ => Redirect(UserAuthUrl))(action)

  def auth(code: String) = Action { implicit request =>
    Async {
      WS.url(AccessTokenExchangeUrl(code)).post("").map { response =>
        response.body match {
          case AccessTokenExchangeResponsePattern(accessToken) => {
            Redirect("/").withSession(
              session + (GithubOAuthAccessTokenSessionKey -> accessToken)
            )
          }
          case _ => Unauthorized("Unknown OAuth State")
        }
      }
    }
  }
}
