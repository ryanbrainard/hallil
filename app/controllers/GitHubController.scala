package controllers

import play.api.mvc._
import services.GitHubApi
import collection.immutable.Map
import collection.Seq
import models.{View, Issue, Repo}

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
              Ok(views.html.GitHub.issues(allReposWithIssues))
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
              Ok(views.html.GitHub.repos(allRepos.sortBy(_.name)))
          }
        }
      }
  }
}