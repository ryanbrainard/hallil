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
          val repoNames = Json.parse[Seq[String]](Redis.exec(_.hget(accessToken, "repoNames")))


          
          GitHubApi(accessToken).getAllRepos().map {
            (allRepos: Seq[Repo]) =>
              val form: Form[Seq[String]] = selectReposForm.fill(allRepos.map(repo => repo.owner.login + "/" + repo.name))
              Ok(views.html.GitHub.repos(form))
          }
        }
      }
  }

  def selectRepos = OAuthController.using(GitHubApi) {
    accessToken =>
      Action(parse.urlFormEncoded) {
        request =>
          val repoNames: Seq[String] = request.body("repoNames")

          Redis.exec(_.hset(accessToken, "repoNames", Json.generate(repoNames)))

          Ok("Saved!")
      }
  }
}
