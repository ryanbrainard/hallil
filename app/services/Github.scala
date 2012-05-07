package services

import play.api.libs.ws.WS
import play.api.libs.concurrent.Promise
import com.codahale.jerkson.Json
import models.{Repo, Issue}
import collection.Seq
import collection.immutable.{Map, List}

/**
 * @author Ryan Brainard
 */

object GitHub extends OAuthService {

  private val baseApiUrl = "https://api.github.com"
  private val clientId = sys.env.getOrElse("GITHUB_CLIENT_ID", sys.error("GITHUB_CLIENT_ID not configured"))
  private val clientSecret = sys.env.getOrElse("GITHUB_CLIENT_SECRET", sys.error("GITHUB_CLIENT_SECRET not configured"))

  def userAuthUrl =  "https://github.com/login/oauth/authorize?client_id=%s".format(clientId)

  def exchangeCodeForAccessToken(code: String): Promise[String] = {
    val accessTokenExchangeResponsePattern = """.*access_token=(\w+).*""".r

    def accessTokenExchangeUrl(code: String) = "https://github.com/login/oauth/access_token?client_id=%s&client_secret=%s&code=%s"
      .format(clientId, clientSecret, code)

    WS.url(accessTokenExchangeUrl(code)).post("").map {
      response =>
        response.body match {
          case accessTokenExchangeResponsePattern(accessToken) => accessToken
          case _ => sys.error("OAuth exchange response does not match expected pattern")
        }
    }
  }

  private def get[A](accessToken: String, url: String)(implicit mf: Manifest[A]) = {
    val fullUrl = if (url.startsWith("http")) url else baseApiUrl + url

    WS.url(fullUrl).withHeaders(("Authorization", "token " + accessToken)).get().map {
      response =>
        Json.parse[A](response.body)(mf)
    }
  }

  def getRepos(accessToken: String): Promise[Seq[Repo]] = get[Seq[Repo]](accessToken, "/user/repos")

  def getIssues(accessToken: String): Promise[Seq[Issue]] = get[Seq[Issue]](accessToken, "/issues")
  def getIssues(accessToken: String, repo: Repo): Promise[Seq[Issue]] = get[Seq[Issue]](accessToken, repo.url + "/issues")

  def getAllIssues(accessToken: String) = {
    getRepos(accessToken).flatMap { allRepos =>
      val reposWithIssues: Seq[Repo] = allRepos.filter(_.has_issues)

      val reposToIssues: Seq[Promise[(Repo, Seq[Issue])]] = reposWithIssues.map { repo =>
        getIssues(accessToken, repo).map {
          issues =>
            (repo, issues)
        }
      }

      Promise.sequence(reposToIssues).map(_.toMap)
    }
  }
}
