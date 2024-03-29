package models

/**
 * @author Ryan Brainard
 */

trait CanOwnRepo

case class Organization(login: String) extends CanOwnRepo

object AuthenticatedUser extends CanOwnRepo

case class Owner(login: String)

case class Repo(name: String, url: String, has_issues: Boolean, owner: Owner, open_issues: Int, html_url: String) extends Ordered[Repo] {
  def compare(that: Repo) = name.compare(that.name)
  def toCanonicalName = owner.login + "/" + name
}

case class Issue(number: Int, title: String, state: String,html_url: String)

case class View(repoNames: List[String])