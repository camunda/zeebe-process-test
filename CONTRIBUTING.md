# Contribution Guide

* [Request feature or report issues](#request-feature-or-report-issues)
* [GitHub Issue Guidelines](#github-issue-guidelines)
* [Starting on an Issue](#starting-on-an-issue)
* [Creating a Pull Request](#creating-a-pull-request)
* [Commit Message Guidelines](#commit-message-guidelines)
* [Contributor License Agreement](#contributor-license-agreement)
* [Licenses](#licenses)
* [Code of Conduct](#code-of-conduct)

## Request feature or report issues

We use GitHub issues to organize the development process. If you want to
report a bug or request a new feature feel free to open a new issue on
[GitHub][issues].

If you are reporting a bug, please help to speed up problem diagnosis by
providing as much information as possible. Ideally, this would include the process being tested and
the test code.

## GitHub Issue Guidelines

Every issue should have a meaningful name and a description which either
describes:
- a new feature with details about the use case the feature would solve or
  improve
- a problem, how we can reproduce it and what would be the expected behavior

## Starting on an issue

To work on an issue, follow the following steps:

1. Check that a [GitHub issue][issues] exists for the task you want to work on. If this is not the
   case, please create one.
2. Checkout the `main` branch and pull the latest changes.
   ```
   git checkout main
   git pull
   ```
3. Create a new branch with the naming scheme `issueId-description`.
   ```
   git checkout -b 123-add-process-assertion`
   ```
4. Follow the [Google Java Format](https://github.com/google/google-java-format#intellij-android-studio-and-other-jetbrains-ides)
   and [Zeebe Code Style](https://github.com/zeebe-io/zeebe/wiki/Code-Style) while coding.
5. Implement the required changes on your branch and regularly push your
   changes to the origin so that the CI can run.
   ```
   git commit -am 'feat: add process assertion'
   git push -u origin 123-add-process-assertion
   ```
6. If you think you finished the issue please prepare the branch for reviewing.
   Please squash commits into meaningful commits with a helpful message. This means cleanup/fix etc
   commits should be squashed into the related commit. If you made refactorings
   it would be best if they are split up into another commit. Rule of thumb is
   that you should think about how a reviewer can best understand your changes.
   Please follow the [commit message guidelines](#commit-message-guidelines).
7. After finishing up the squashing force push your changes to your branch.
   ```
   git push --force-with-lease
   ```

## Creating a pull request

1. To start the review process create a new pull request on GitHub from your
   branch to the `main` branch. Give it a meaningful name and describe
   your changes in the body of the pull request. Lastly add a link to the issue
   this pull request closes, i.e. by writing in the description `closes #123`
2. Assign the pull request to one developer to review, if you are not sure who
   should review the issue skip this step. Someone will assign a reviewer for
   you.
3. The reviewer will look at the pull request in the following days and give
   you either feedback or accept the changes. Your reviewer might use
   [emoji code](https://devblogs.microsoft.com/appcenter/how-the-visual-studio-mobile-center-team-does-code-review/#introducing-the-emoji-code)
   during the reviewing process.
  1. If there are changes requested address them in a new commit. Notify the
     reviewer in a comment if the pull request is ready for review again. If
     the changes are accepted squash them again in the related commit and force push.
  2. If no changes are requested the reviewer will merge your changes.

## Commit Message Guidelines

Commit messages use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/#summary) format.

```
<header>
<BLANK LINE>
<body>
<BLANK LINE> (optional - mandatory with footer)
<footer> (optional)
```


### Commit message header
Examples:

* `docs: add commit styles to contribution guide`
* `feat: add assertion for processes`
* `fix: prevent nullpointer on assertion`

The commit header should match the following pattern:
```
%{type}: %{description}
```

The commit header should be kept short, preferably under 72 chars but we allow a max of 120 chars.

- `type` should be one of:
  - `build`: Changes that affect the build system (e.g. Maven, Docker, etc)
  - `ci`: Changes to our CI configuration files and scripts (e.g. GitHub Workflows)
  - `deps`: A change to the external dependencies (was already used by Dependabot)
  - `docs`:  A change to the documentation
  - `feat`: A new feature (both internal or user-facing)
  - `fix`: A bug fix (both internal or user-facing)
  - `perf`: A code change that improves performance
  - `refactor`: A code change that does not change the behavior
  - `style`: A change to align the code with our style guide
  - `test`: Adding missing tests or correcting existing tests
- `description`: short description of the change in present tense

### Commit message body

Should describe the motivation for the change.

## Contributor License Agreement

You will be asked to sign our Contributor License Agreement when you open a Pull Request. We are not
asking you to assign copyright to us, but to give us the right to distribute
your code without restriction. We ask this of all contributors in order to
assure our users of the origin and continuing existence of the code. You only
need to sign the CLA once.

## Licenses

Source files are made available under the [Apache License, Version 2.0](/licenses/APACHE-2.0.txt).

## Code of Conduct

This project adheres to the [Camunda Code of Conduct](https://camunda.com/events/code-conduct/).
By participating, you are expected to uphold this code. Please [report](https://camunda.com/events/code-conduct/reporting-violations/)
unacceptable behavior as soon as possible.

[issues]: https://github.com/camunda-cloud/zeebe-process-test/issues
