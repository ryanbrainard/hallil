package controllers

import play.api.mvc._
import services.{TrelloApi, GitHubApi}

object TrelloController extends Controller {

  def boards = OAuthController.using(TrelloApi) {
    implicit oauthAccess =>
      implicit request: Request[AnyContent] =>
        Async {
          TrelloApi().getBoards().map {
            boards =>
              Ok(boards.toString())
          }
        }
  }
}
