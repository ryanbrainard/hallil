package services

import play.api.libs.ws.WS
import play.api.libs.concurrent.Promise
import com.codahale.jerkson.Json
import collection.Seq
import collection.immutable.{Map, List}
import models._

/**
 * @author Ryan Brainard
 */

object GitHub extends OAuthService {

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
  
  def apply(accessToken: String) = new GitHub(accessToken)
}

class GitHub(accessToken: String) {

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
        val allPromisesOfReposWithTheirIssues: Seq[Promise[Map[Repo, Seq[Issue]]]] = owners.map {
          owner =>
            getAllIssuesFor(owner)
        }

        Promise.sequence(allPromisesOfReposWithTheirIssues).map {
          allReposWithTheirIssues =>
            allReposWithTheirIssues.reduceLeft {
              (a, b) =>
                a ++ b
            }
        }
    }
  }
  
  private def getAllOwners(): Promise[Seq[CanOwnRepo]] = getOrgs().map(_:+ AuthenticatedUser)

  private def getAllIssuesFor(owner: CanOwnRepo): Promise[Map[Repo, Seq[Issue]]] = {
    owner match {
      case org: Organization => getAllIssuesIn(getRepos(org))
      case user: AuthenticatedUser.type => getAllIssuesIn(getRepos())
    }
  }

  private def getAllIssuesIn(reposP: Promise[Seq[Repo]]) = reposP.flatMap {
    allRepos =>
      val reposWithIssues: Seq[Repo] = allRepos.filter(_.has_issues)

      val reposToIssues: Seq[Promise[(Repo, Seq[Issue])]] = reposWithIssues.map { repo =>
        getIssues(repo).map {
          issues =>
            (repo, issues)
        }
      }

      Promise.sequence(reposToIssues).map(_.toMap)
  }
}
