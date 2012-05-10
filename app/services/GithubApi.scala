package services

import play.api.libs.ws.WS
import play.api.libs.concurrent.Promise
import com.codahale.jerkson.Json
import collection.immutable.Map
import models._
import collection.{SortedMap, Seq}

/**
 * @author Ryan Brainard
 */

object GitHubApi extends OAuthService {

  private val clientId = sys.env.getOrElse("GITHUB_CLIENT_ID", sys.error("GITHUB_CLIENT_ID not configured"))
  private val clientSecret = sys.env.getOrElse("GITHUB_CLIENT_SECRET", sys.error("GITHUB_CLIENT_SECRET not configured"))

  def userAuthUrl =  "https://github.com/login/oauth/authorize?client_id=%s&scope=repo".format(clientId)

  def exchangeCodeForAccessToken(code: String): Promise[String] = {
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
  
  def apply(accessToken: String) = new GitHubApi(accessToken)
}

class GitHubApi(accessToken: String) {

  private val baseApiUrl = "https://api.github.com"
  
  private def get[A](url: String)(implicit mf: Manifest[A]) = {
    val fullUrl = if (url.startsWith("http")) url else baseApiUrl + url

    WS.url(fullUrl).withHeaders(("Authorization", "token " + accessToken)).get().map {
      response =>
        Json.parse[A](response.body)(mf)
    }
  }

  def getOrgs(): Promise[Seq[Organization]] = get[Seq[Organization]]("/user/orgs")

  def getRepos(): Promise[Seq[Repo]] = get[Seq[Repo]]("/user/repos")

  def getRepos(org: Organization): Promise[Seq[Repo]] = get[Seq[Repo]]("/orgs/" + org.login + "/repos")

  def getIssues(): Promise[Seq[Issue]] = get[Seq[Issue]]("/issues")

  def getIssues(repo: Repo): Promise[Seq[Issue]] = get[Seq[Issue]](repo.url + "/issues")

  def getAllReposWithIssues(): Promise[Map[Repo, Seq[Issue]]] = {
    getAllOwners().flatMap {
      owners =>
        Promise.sequence(owners.map {
          owner =>
            getAllReposFor(owner).flatMap {
              allRepos => getAllIssuesIn(allRepos.filter(_.open_issues > 0))
            }
        }).map {
          allReposWithTheirIssues =>
            val reduced: Map[Repo, Seq[Issue]] = allReposWithTheirIssues.reduceLeft {
              (a, b) =>
                (a ++ b)
            }
            SortedMap(reduced.toSeq: _*).toMap
        }
    }
  }
  
  private def getAllOwners(): Promise[Seq[CanOwnRepo]] = getOrgs().map(_:+ AuthenticatedUser)

  private def getAllReposFor(owner: CanOwnRepo): Promise[Seq[Repo]] = owner match {
      case org: Organization => getRepos(org)
      case user: AuthenticatedUser.type => getRepos()
  }

  private def getAllIssuesIn(repos: Seq[Repo]): Promise[Map[Repo, Seq[Issue]]] = {
    Promise.sequence(repos.filter(_.has_issues).map {
      repo =>
        getIssues(repo).map {
          issues =>
            (repo, issues)
        }
    }).map(_.toMap)
  }
}
