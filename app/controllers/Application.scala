package controllers

import play.api.mvc._
import services.GitHub
import com.codahale.jerkson.Json
import collection.immutable.Map
import collection.Seq
import models.{Issue, Repo}

object Application extends Controller {

  def index = OAuth.using(GitHub) {
    accessToken =>
      Action {
        Ok(accessToken)
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