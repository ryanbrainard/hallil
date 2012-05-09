package models

/**
 * @author Ryan Brainard
 */

case class Repo(name: String, url: String, has_issues: Boolean, owner: Owner, open_issues: Int) extends Ordered[Repo]{
  def compare(that: Repo) = name.compare(that.name)
}

case class Owner(login: String)