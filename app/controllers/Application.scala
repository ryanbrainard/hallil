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
  
  def logout = Action {
    Ok("Logged out").withNewSession
  }
}