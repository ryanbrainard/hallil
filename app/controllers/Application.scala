package controllers

import play.api.mvc._
import services.GitHub
import collection.immutable.Map
import models.{Issue, Repo}
import collection.Seq

object Application extends Controller {

  def index = OAuth.using(GitHub) {
    accessToken =>
      Action {
        Ok(views.html.Application.index())
      }
  }

  def issues = OAuth.using(GitHub) {
    accessToken =>
      Action {
        Async {
          GitHub(accessToken).getAllReposWithIssues().map {
            (allReposWithIssues: Map[Repo, Seq[Issue]]) =>
              Ok(views.html.Application.issues(allReposWithIssues))
          }
        }
      }
  }

  def logout = Action {
    Ok("Logged out").withNewSession
  }
}