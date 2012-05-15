package controllers

import play.api.mvc._
import collection.immutable.Map
import services.{Redis, GitHubApi}
import com.codahale.jerkson.Json
import controllers.OAuthController.OAuthAccess
import play.api.libs.concurrent.Promise
import collection.{SortedMap, Seq}
import models._

object GitHubController extends Controller {

  def key = OAuthController.using(GitHubApi) {
    oauthAccess =>
      Action(Ok(oauthAccess.token))
  }

  def issues = OAuthController.using(GitHubApi) {
    implicit oauthAccess =>
      implicit request: Request[AnyContent] =>
        Async {
          getAllIssuesInNamed(getSelectedRepos(oauthAccess)).map {
            (selectedReposWithIssues: Map[String, Seq[Issue]]) =>
              Ok(views.html.GitHub.issues(selectedReposWithIssues))
          }
        }
  }

  def displayReposWithIssuesForSelection = OAuthController.using(GitHubApi) {
    implicit access =>
      Action {
        Async {
          getAllRepos().map {
            (allRepos: Seq[Repo]) =>
              val allReposWithSelections: Map[String, Boolean] = allRepos.filter(r => r.has_issues).map {
                repo =>
                  val repoName = repo.toCanonicalName
                  (repoName, getSelectedRepos(access).contains(repoName))
              }.toMap

              Ok(views.html.GitHub.repos(allReposWithSelections))
          }
        }
      }
  }

  def selectRepos = {
    implicit val parser = parse.urlFormEncoded
    OAuthController.using(GitHubApi) {
      implicit access =>
        implicit request: Request[Map[String, Seq[String]]] =>
          if (request.body.contains("selectedRepos")) {
            // bulk / total replacement
            val selectedRepos: Seq[String] = request.body("selectedRepos")
            Redis.exec(_.hset(access.token, "selectedRepos", Json.generate(selectedRepos)))
          } else if (request.body.contains("repoName")) {
            // single / additive
            val repoName: String = request.body("repoName").head

            GitHubApi().getRepo(repoName).value.fold(
              e => {
                // ignore
                // TODO: display error msg
              },
              s => {
                val newSelectedRepos: Seq[String] = (getSelectedRepos(access).toSet + repoName).toSeq
                Redis.exec(_.hset(access.token, "selectedRepos", Json.generate(newSelectedRepos)))
              }
            )
          }

          Redirect("/github/issues")
    }
  }

  def unselectRepo(owner: String, name: String) = OAuthController.using(GitHubApi) {
    implicit access =>
      Action {
        request =>
          val repoNameToRemove = owner + "/" + name
          val newSelectedRepos: Seq[String] = (getSelectedRepos(access).toSet - repoNameToRemove).toSeq
          Redis.exec(_.hset(access.token, "selectedRepos", Json.generate(newSelectedRepos)))
          Redirect("/github/issues")
      }
  }

  private def getSelectedRepos(implicit oauthAccess: OAuthAccess[GitHubApi.type]) = {
    val rawSelectedRepos = Redis.exec(_.hget(oauthAccess.token, "selectedRepos"))
    Option(rawSelectedRepos).map(r => Json.parse[Seq[String]](r)).getOrElse(Seq())
  }

  private def getAllRepos()(implicit access: OAuthAccess[GitHubApi.type]) = {
    getAllOwners().flatMap {
      owners =>
        Promise.sequence(owners.map {
          owner =>
            getAllReposFor(owner)
        }).map(s => s.flatMap(t => t)) //TODO: clean up this transformation
    }
  }

  private def getAllReposWithTheirIssues()(implicit access: OAuthAccess[GitHubApi.type]): Promise[Map[Repo, Seq[Issue]]] = {
    getAllOwners().flatMap {
      owners =>
        Promise.sequence(owners.map {
          owner =>
            getAllReposFor(owner).flatMap {
              allRepos => getAllIssuesIn(allRepos)
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

  private def getAllOwners()(implicit access: OAuthAccess[GitHubApi.type]): Promise[Seq[CanOwnRepo]] = GitHubApi().getOrgs().map(_ :+ AuthenticatedUser)

  private def getAllReposFor(owner: CanOwnRepo)(implicit access: OAuthAccess[GitHubApi.type]): Promise[Seq[Repo]] = owner match {
    case org: Organization => GitHubApi().getRepos(org)
    case user: AuthenticatedUser.type => GitHubApi().getRepos()
  }

  private def getAllIssuesInNamed(repoNames: Seq[String])(implicit access: OAuthAccess[GitHubApi.type]): Promise[Map[String, Seq[Issue]]] = {
    Promise.sequence(repoNames.map {
      repoName =>
        GitHubApi().getIssues(repoName).map {
          issues =>
            (repoName, issues.sortBy(issue => issue.number))
        }
    }).map(_.toMap)
  }

  //todo: re-dupe with above
  private def getAllIssuesIn(repos: Seq[Repo])(implicit access: OAuthAccess[GitHubApi.type]): Promise[Map[Repo, Seq[Issue]]] = {
    Promise.sequence(repos.filter(_.has_issues).map {
      repo =>
        GitHubApi().getIssues(repo).map {
          issues =>
            (repo, issues.sortBy(issue => issue.number))
        }
    }).map(_.toMap)
  }
}
