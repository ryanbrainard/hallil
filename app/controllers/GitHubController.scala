package controllers

import play.api.mvc._
import services.GitHubApi
import collection.immutable.Map
import models.{Issue, Repo}
import collection.Seq

object GitHubController extends Controller {

  def key = OAuthController.using(GitHubApi) {
    accessToken =>
      Action(Ok(accessToken))
  }
  
  def issues = OAuthController.using(GitHubApi) {
    accessToken =>
      Action {
        Async {
          GitHubApi(accessToken).getAllReposWithIssues().map {
            (allReposWithIssues: Map[Repo, Seq[Issue]]) =>
              Ok(views.html.GitHub.issues(allReposWithIssues))
          }
        }
      }
  }
}