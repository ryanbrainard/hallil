package controllers

import play.api.mvc._
import collection.immutable.Map
import collection.Seq
import play.api.data._
import play.api.data.Forms._
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
          GitHubApi(accessToken).getAllReposWithTheirIssues().map {
          
            (allReposWithIssues: Map[Repo, Seq[Issue]]) =>
              Ok(views.html.GitHub.issues(allReposWithIssues.filterKeys(r => getSelectedRepos(accessToken).contains(r.toCanonicalName))))
          }
        }
      }
  }

  def repos = OAuthController.using(GitHubApi) {
    accessToken =>
      Action {
        Async {
          GitHubApi(accessToken).getAllRepos().map {
            (allRepos: Seq[Repo]) =>
              val allReposWithSelections: Map[String, Boolean] = allRepos.map {
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

          Ok("Saved!")
      }
  }
  
  def getSelectedRepos(accessToken: String) = {
    val rawSelectedRepos = Redis.exec(_.hget(accessToken, "selectedRepos"))
    Option(rawSelectedRepos).map(r => Json.parse[Seq[String]](r)).getOrElse(Seq())
  }
}
