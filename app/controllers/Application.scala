package controllers

import play.api.mvc._
import services.GitHub

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
          GitHub.issues(accessToken).map {
            issues =>

              Ok(issues.toString)
          }
        }
      }
  }

  def logout = Action {
    Ok("Logged out").withNewSession
  }
}