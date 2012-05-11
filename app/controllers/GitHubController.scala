package controllers

import play.api.mvc._
import services.GitHubApi
import collection.immutable.Map
import collection.Seq
import play.api.data._
import play.api.data.Forms._
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

  val selectReposForm = Form(
    single(
      "repoNames" -> seq(text)
    )
  )

  def repos = OAuthController.using(GitHubApi) {
    accessToken =>
      Action {
        Async {
          GitHubApi(accessToken).getAllRepos().map {
            (allRepos: Seq[Repo]) =>
              val form: Form[Seq[String]] = selectReposForm.fill(allRepos.map(repo => repo.owner.login + "/" + repo.name))
              Ok(views.html.GitHub.repos(form))
          }
        }
      }
  }
 
  def selectRepos = Action(parse.urlFormEncoded) { request =>
      val repoNames: Seq[String] = request.body("repoNames")



      Ok(repoNames.toString)
  }
}