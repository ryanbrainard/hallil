package models

/**
 * @author Ryan Brainard
 */

trait CanOwnRepo

case class Organization(login: String) extends CanOwnRepo

object AuthenticatedUser extends CanOwnRepo