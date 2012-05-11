package controllers

import play.api.mvc._
import collection.immutable.Map
import collection.Seq
import models.{Issue, Repo}
import services.{Redis, GitHubApi}
import com.codahale.jerkson.Json

object GitHubController extends Controller {

  def key = OAuthController.using(GitHubApi) {
    accessToken =>
      Action(Ok(accessToken))
  }

  def issues = OAuthController.using(GitHubApi) {
    accessToken =>
      Action {
        Async {
          GitHubApi(accessToken).getAllIssuesInNamed(getSelectedRepos(accessToken)).map {
            (selectedReposWithIssues: Map[String, Seq[Issue]]) =>
              Ok(views.html.GitHub.issues(selectedReposWithIssues))
          }
        }
      }
  }

  def displayReposWithIssuesForSelection = OAuthController.using(GitHubApi) {
    accessToken =>
      Action {
        Async {
          GitHubApi(accessToken).getAllRepos().map {
            (allRepos: Seq[Repo]) =>
              val allReposWithSelections: Map[String, Boolean] = allRepos.filter(r => r.has_issues).map {
                repo =>
                  val repoName = repo.toCanonicalName
                  (repoName, getSelectedRepos(accessToken).contains(repoName))
              }.toMap

              Ok(views.html.GitHub.repos(allReposWithSelections))
          }
        }
      }
  }

  def selectRepos = OAuthController.using(GitHubApi) {
    accessToken =>
      Action(parse.urlFormEncoded) {
        request =>
          val selectedRepos: Seq[String] = request.body("selectedRepos")
          Redis.exec(_.hset(accessToken, "selectedRepos", Json.generate(selectedRepos)))
          Redirect("/github/issues")
      }
  }

  def unselectRepo(owner: String, name: String) = OAuthController.using(GitHubApi) {
    accessToken =>
      Action {
        request =>
          val repoNameToRemove = owner + "/" + name
          val newSelectedRepos: Seq[String] = (getSelectedRepos(accessToken).toSet - repoNameToRemove).toSeq
          Redis.exec(_.hset(accessToken, "selectedRepos", Json.generate(newSelectedRepos)))
          Redirect("/github/issues")
      }
  }
  
  def getSelectedRepos(accessToken: String) = {
    val rawSelectedRepos = Redis.exec(_.hget(accessToken, "selectedRepos"))
    Option(rawSelectedRepos).map(r => Json.parse[Seq[String]](r)).getOrElse(Seq())
  }
}
