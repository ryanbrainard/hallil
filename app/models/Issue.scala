package models

/**
 * @author Ryan Brainard
 */

case class Issue(id: String,
                       state: String,
                       title: String,
                       html_url: String
                       ) {
}

/*
[
{
  "html_url": "https://github.com/heroku/heroku-eclipse-plugin/issues/2",
  "state": "open",
  "labels": [

  ],
  "user": {
    "gravatar_id": "acb0522d40afbebb3023adde40812e0e",
    "url": "https://api.github.com/users/anandbn",
    "avatar_url": "https://secure.gravatar.com/avatar/acb0522d40afbebb3023adde40812e0e?d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-140.png",
    "id": 355297,
    "login": "anandbn"
  },
  "created_at": "2012-04-30T18:58:17Z",
  "url": "https://api.github.com/repos/heroku/heroku-eclipse-plugin/issues/2",
  "pull_request": {
    "html_url": null,
    "diff_url": null,
    "patch_url": null
  },
  "closed_at": null,
  "body": "Setup EC2 instance and Jenkins CI. \r\n\r\n@jsimone  - Can you add additional details on actual AMI requirements/Jenkins requirements",
  "updated_at": "2012-05-06T15:57:31Z",
  "repository": {
    "mirror_url": null,
    "forks": 2,
    "svn_url": "https://github.com/heroku/heroku-eclipse-plugin",
    "html_url": "https://github.com/heroku/heroku-eclipse-plugin",
    "description": "Heroku Eclipse plugin is a fully integrated plugin into Eclipse IDE that will allow developers to manage their Heroku apps and environment right from their favourite IDE",
    "open_issues": 2,
    "language": "Java",
    "clone_url": "https://github.com/heroku/heroku-eclipse-plugin.git",
    "pushed_at": "2012-05-04T18:59:22Z",
    "created_at": "2012-04-11T00:39:13Z",
    "url": "https://api.github.com/repos/heroku/heroku-eclipse-plugin",
    "git_url": "git://github.com/heroku/heroku-eclipse-plugin.git",
    "fork": false,
    "homepage": "",
    "has_downloads": true,
    "watchers": 4,
    "ssh_url": "git@github.com:heroku/heroku-eclipse-plugin.git",
    "updated_at": "2012-05-06T15:33:36Z",
    "size": 1795,
    "private": true,
    "owner": {
    "gravatar_id": "e327d6043ba309e2bfc8b56110b6dbbe",
    "url": "https://api.github.com/users/heroku",
    "avatar_url": "https://secure.gravatar.com/avatar/e327d6043ba309e2bfc8b56110b6dbbe?d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-orgs.png",
    "id": 23211,
    "login": "heroku"
  },
    "name": "heroku-eclipse-plugin",
    "id": 3988708,
    "has_wiki": true,
    "has_issues": true
  },
  "comments": 3,
  "milestone": null,
  "number": 2,
  "assignee": {
    "gravatar_id": "4293f5d9cfe7ef6b69f44173746369df",
    "url": "https://api.github.com/users/ryanbrainard",
    "avatar_url": "https://secure.gravatar.com/avatar/4293f5d9cfe7ef6b69f44173746369df?d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-140.png",
    "id": 966764,
    "login": "ryanbrainard"
  },
  "id": 4355729,
  "title": "Setup EC2 instance for CI for Plugin"
}
]
*/