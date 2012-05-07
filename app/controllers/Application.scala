package controllers

import play.api.mvc._

object Application extends Controller {

  def index = Github.Authenticated {
    oauthToken =>
      Action {
        Ok(oauthToken)
      }
  }
  
  def logout = Action {
    Ok("Logged out").withNewSession
  }

}