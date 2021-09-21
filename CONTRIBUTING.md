# Contributing

When contributing to this repository, please first discuss the change you wish to make via issue,
gitter with the owners of this repository before making a change.

Please note we have a code of conduct, please follow it in all your interactions with the project.

## Pull Request Process

1. Ensure any install or build dependencies are removed before the end of the layer when doing a 
   build.
2. Update the README.md with details of changes to the interface, this includes new environment 
   variables, exposed ports, useful file locations and container parameters.
3. Increase the version numbers in any examples files and the README.md to the new version that this
   Pull Request would represent. The versioning scheme we use is [SemVer](http://semver.org/).
4. You may merge the Pull Request in once you have the sign-off of repository owner, or if you 
   do not have permission to do that, you may request the second reviewer to merge it for you.

## Code of Conduct

### Our Pledge

In the interest of fostering an open and welcoming environment, we as
contributors and maintainers pledge to making participation in our project and
our community a harassment-free experience for everyone, regardless of age, body
size, disability, ethnicity, gender identity and expression, level of experience,
nationality, personal appearance, race, religion, or sexual identity and
orientation.

### Our Standards

Examples of behavior that contributes to creating a positive environment
include:

* Using welcoming and inclusive language
* Being respectful of differing viewpoints and experiences
* Gracefully accepting constructive criticism
* Focusing on what is best for the community
* Showing empathy towards other community members

Examples of unacceptable behavior by participants include:

* Trolling, insulting/derogatory comments, and personal or political attacks
* Public or private harassment
* Publishing others' private information, such as a physical or electronic
  address, without explicit permission
* Other conduct which could reasonably be considered inappropriate in a
  professional setting

### Our Responsibilities

Project maintainers are responsible for clarifying the standards of acceptable
behavior and are expected to take appropriate and fair corrective action in
response to any instances of unacceptable behavior.

Project maintainers have the right and responsibility to remove, edit, or
reject comments, commits, code, wiki edits, issues, and other contributions
that are not aligned to this Code of Conduct, or to ban temporarily or
permanently any contributor for other behaviors that they deem inappropriate,
threatening, offensive, or harmful.

### Scope

This Code of Conduct applies both within project spaces and in public spaces
when an individual is representing the project or its community. Examples of
representing a project or community include using an official project e-mail
address, posting via an official social media account, or acting as an appointed
representative at an online or offline event. Representation of a project may be
further defined and clarified by project maintainers.

### Attribution

This Code of Conduct is adapted from the [Contributor Covenant][homepage], version 1.4,
available at [http://contributor-covenant.org/version/1/4][version]

[homepage]: http://contributor-covenant.org
[version]: http://contributor-covenant.org/version/1/4/

You want to contribute? Welcome! Please read this document to understand what you can do:
 * [Analyze Issues](#analyze-issues)
 * [Report an Issue](#report-an-issue)
 * [Contribute Code](#contribute-code)

## Analyze Issues

Analyzing issue reports can be a lot of effort. Any help is welcome!
Go to [the Github issue tracker](https://github.com/SAP/hybris-commerce-eclipse-plugin/issues?state=open) and find an open issue which needs additional work or a bugfix.

Additional work may be further information, or a minimized jsbin example or gist, or it might be a hint that helps understanding the issue. Maybe you can even find and [contribute](#contribute-code) a bugfix?


## Report an Issue

If you find a bug you are welcome to report it.
We can only handle well-reported, actual bugs, so please follow the guidelines below for support questions or when in doubt whether the issue is an actual bug.

Once you have familiarized with the guidelines, you can go to the [Github issue tracker](https://github.com/SAP/hybris-commerce-eclipse-plugin/issues/new) to report the issue.

### Quick Checklist for Bug Reports

Issue report checklist:
 * Real, current bug
 * No duplicate
 * Reproducible
 * Good summary
 * Well-documented
 * Minimal example
 * Use the [template](ISSUE_TEMPLATE.md)


### Requirements for a bug report

These requirements are the mandatory base of a good bug report:
1. No duplicate: you have searched the issue tracker to make sure the bug has not yet been reported
2. Good summary: the summary should be specific to the issue
3. Current bug: the bug can be reproduced in the most current version (state the tested version!)
4. Reproducible bug: there are clear steps to reproduce given. This includes:
   * a URL to access the example
   * any required user/password information (do not reveal any credentials that could be mis-used!)
   * detailed and complete step-by-step instructions to reproduce the bug
5. Precise description:
   * precisely state the expected and the actual behavior
   * give information about the used browser/device and its version, if possible also the behavior in other browsers/devices
   * if the bug is about wrong UI appearance, attach a screenshot and mark what is wrong
   * generally give as much additional information as possible. (But find the right balance: do not invest hours for a very obvious and easy to solve issue. When in doubt, give more information.)
6. Only one bug per report: open different tickets for different issues

Please report bugs in English, so all users can understand them.

### Issue handling process

When an issue is reported, a committer will look at it and either confirm it as a real issue (by giving the "in progress" label), close it if it is not an issue, or ask for more details. In-progress issues are then either assigned to a committer in GitHub, reported in our internal issue handling system, or left open as "contribution welcome" for easy or not urgent fixes.

An issue that is about a real bug is closed as soon as the fix is committed. The closing comment explains which patch version(s) will contain the fix.


### Usage of Labels

Github offers labels to categorize issues. We defined the following labels so far:

Labels for issue categories:
 * bug: this issue is a bug in the code
 * documentation: this issue is about wrong documentation
 * enhancement: this is not a bug report, but an enhancement request

Status of open issues:
 * unconfirmed: this report needs confirmation whether it is really a bug (no label; this is the default status)
 * in progress: this issue has been triaged and is now being handled, e.g. because it looks like an actual bug
 * author action: the author is required to provide information
 * contribution welcome: this fix/enhancement is something we would like to have and you are invited to contribute it

Status/resolution of closed issues:
 * fixed: a fix for the issue was provided
 * duplicate: the issue is also reported in a different ticket and is handled there
 * invalid: for some reason or another this issue report will not be handled further (maybe lack of information or issue does not apply anymore)
 * works: not reproducible or working as expected
 * wontfix: while acknowledged to be an issue, a fix cannot or will not be provided

The labels can only be set and modified by committers.


### Issue Reporting Disclaimer

We want to improve the quality of product and good bug reports are welcome! But our capacity is limited, so we cannot handle questions or consultation requests and we cannot afford to ask for required details. So we reserve the right to close or to not process insufficient bug reports in favor of those which are very cleanly documented and easy to reproduce. Even though we would like to solve each well-documented issue, there is always the chance that it won't happen - remember: our solution is Open Source and comes without warranty.

Bug report analysis support is very welcome! (e.g. pre-analysis or proposing solutions)


## Contribute Code

You are welcome to contribute code in order to fix bugs or to implement new features.

There are three important things to know:

1.  You must be aware of the Apache License (which describes contributions) and **agree to the Developer Certificate of Origin**. This is common practice in all major Open Source projects. To make this process as simple as possible, we are using *[CLA assistant](https://cla-assistant.io/)*. CLA assistant is an open source tool that integrates with GitHub very well and enables a one-click-experience for accepting the DCO. See the respective section below for details.
2.  There are **several requirements regarding code style, quality, and product standards** which need to be met (we also have to follow them). The respective section below gives more details on the coding guidelines.
3.  **Not all proposed contributions can be accepted**. Some features may e.g. just fit a third-party add-on better. For most bug fixes this is a given, but major feature implementation first need to be discussed with committers (the top 3 or more of the [Contributors List](https://github.com/SAP/hybris-commerce-eclipse-plugin/graphs/contributors)), possibly one who touched the related code recently. The more effort you invest, the better you should clarify in advance whether the contribution fits: the best way would be to just open an enhancement ticket in the issue tracker to discuss the feature you plan to implement (make it clear you intend to contribute). We will then forward the proposal to the respective code owner, this avoids disappointment.


### Developer Certificate of Origin (DCO)

Due to legal reasons, contributors will be asked to accept a DCO before they submit the first pull request to this project. SAP uses [the standard DCO text of the Linux Foundation](https://developercertificate.org/).  
This happens in an automated fashion during the submission process: the CLA assistant tool will add a comment to the pull request. Click it to check the DCO, then accept it on the following screen. CLA assistant will save this decision for upcoming contributions.

This DCO replaces the previously used CLA ("Contributor License Agreement") as well as the "Corporate Contributor License Agreement" with new terms which are well-known standards and hence easier to approve by legal departments. Contributors who had already accepted the CLA in the past may be asked once to accept the new DCO.
