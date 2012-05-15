package services

import play.api.libs.ws.WS
import play.api.libs.concurrent.Promise
import models._
import collection.Seq
import play.api.Logger
import com.codahale.jerkson.{ParsingException, Json}
import controllers.OAuthController.OAuthAccess

/**
 * @author Ryan Brainard
 */

object GitHubApi extends OAuth2Service {

  private val clientId = sys.env.getOrElse("GITHUB_CLIENT_ID", sys.error("GITHUB_CLIENT_ID not configured"))
  private val clientSecret = sys.env.getOrElse("GITHUB_CLIENT_SECRET", sys.error("GITHUB_CLIENT_SECRET not configured"))

  def userAuthUrl = "https://github.com/login/oauth/authorize?client_id=%s&scope=repo".format(clientId)

  def retrieveAccessToken(code: String): Promise[String] = {
    val accessTokenExchangeResponsePattern = """.*access_token=(\w+).*""".r

    def accessTokenExchangeUrl(code: String) = "https://github.com/login/oauth/access_token?client_id=%s&client_secret=%s&code=%s"
      .format(clientId, clientSecret, code)

    WS.url(accessTokenExchangeUrl(code)).post("").map {
      response =>
        response.body match {
          case accessTokenExchangeResponsePattern(accessToken) => accessToken
          case _ => sys.error("OAuthController exchange response does not match expected pattern")
        }
    }
  }

  def apply()(implicit access: OAuthAccess[GitHubApi.type]) = new GitHubApi(access)
}

class GitHubApi(access: OAuthAccess[GitHubApi.type]) {

  private val baseApiUrl = "https://api.github.com"

  private def get[A](url: String)(implicit mf: Manifest[A]) = {
    val fullUrl = if (url.startsWith("http")) url else baseApiUrl + url
    Logger.debug("GET " + fullUrl)

    WS.url(fullUrl).withHeaders(("Authorization", "token " + access.token)).get().map {
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

  def getOrgs(): Promise[Seq[Organization]] = get[Seq[Organization]]("/user/orgs")

  def getRepo(repoName: String): Promise[Repo] = get[Repo]("/repos/" + repoName)

  def getRepos(): Promise[Seq[Repo]] = get[Seq[Repo]]("/user/repos")

  def getRepos(org: Organization): Promise[Seq[Repo]] = get[Seq[Repo]]("/orgs/" + org.login + "/repos")

  def getIssues(): Promise[Seq[Issue]] = get[Seq[Issue]]("/issues")

  def getIssues(repo: Repo): Promise[Seq[Issue]] = get[Seq[Issue]](repo.url + "/issues")

  def getIssues(repoName: String): Promise[Seq[Issue]] = get[Seq[Issue]]("/repos/" + repoName + "/issues")

}
