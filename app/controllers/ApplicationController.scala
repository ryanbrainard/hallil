package controllers

import play.api.mvc._
import services.GitHubApi

object ApplicationController extends Controller {

  // TODO: make generic to not just be GitHub
  def index = OAuthController.using(GitHubApi) {
    accessToken =>
      Action {
        Ok(views.html.Application.index())
      }
  }

  def logout = Action {
    Ok("Logged out").withNewSession
  }
}