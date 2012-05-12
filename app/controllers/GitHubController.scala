package controllers

import play.api.mvc._
import collection.immutable.Map
import collection.Seq
import models.{Issue, Repo}
import services.{Redis, GitHubApi}
import com.codahale.jerkson.Json
import controllers.OAuthController.OAuthAccess

object GitHubController extends Controller {

  def key = OAuthController.using(GitHubApi) {
    oauthAccess =>
      Action(Ok(oauthAccess.token))
  }

  def issues = OAuthController.using(GitHubApi) {
    implicit oauthAccess =>
      implicit request: Request[AnyContent] =>
        Async {
          GitHubApi().getAllIssuesInNamed(getSelectedRepos(oauthAccess)).map {
            (selectedReposWithIssues: Map[String, Seq[Issue]]) =>
              Ok(views.html.GitHub.issues(selectedReposWithIssues))
          }
        }
  }

  def displayReposWithIssuesForSelection = OAuthController.using(GitHubApi) {
    implicit access =>
      Action {
        Async {
          GitHubApi().getAllRepos().map {
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
  
  def getSelectedRepos(implicit oauthAccess: OAuthAccess[GitHubApi.type]) = {
    val rawSelectedRepos = Redis.exec(_.hget(oauthAccess.token, "selectedRepos"))
    Option(rawSelectedRepos).map(r => Json.parse[Seq[String]](r)).getOrElse(Seq())
  }
}
