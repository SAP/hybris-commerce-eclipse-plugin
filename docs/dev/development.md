#  Development

## Continuous Integration

folder contains docker configuration for sonarqube installation.
To execute sonar check it is required to run docker installation first

```bash
docker-compose
```

 or by running shell script in this directory

```bash
./start-sonar.sh
```

After running that website will be available on default sonar port [localhost:9000](http://localhost:9000)

## Running Quality Check

To check plugin by sonar, run maven task:

```bash
mvn clean verify sonar:sonar
```

Results will be available on your local website

 ## Generating Changelog Update
 
 Changelog update is done by `gren changelog` command. That is small script which should be installed on your local laptop. More info here https://github.com/github-tools/github-release-notes.
 
 ## Generating Release Notes
 
  [![Automated Release Notes by gren](https://img.shields.io/badge/%F0%9F%A4%96-release%20notes-00B2EE.svg)](https://github-tools.github.io/github-release-notes/)
 
 Release notes, same like changelog is created thanks to *Gren*. To generate release log, please run command:
 
 `gren release --token=<your github token>`



## Incrementing project version

It is required to change project version before releasing latest changes. To do that it is possible to use one of Tycho functionalities for that 

```bash
git flow release start 1.5.0
# create release version
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=1.5.0.RELEASE
# add all changes to git and push to your repository (i.e. feature.xml, MANIFEST.MF and pom.xml)
mvn clean install
rm -rf updatesite/neon && cp -R com.hybris.hyeclipse.site/target/repository updatesite/neon
# push all changes to your git repository
git add updatesite/neon/* updatesite/neon/*/* */*/MANIFEST.MF */pom.xml */feature.xml pom.xml
git commit -m "bumping version to 1.5.0"
# git add <changed files>
git push origin master
git flow release finish
gren release -T <github-token> --tags v1.5.0 v<last-one>
## go to github.com and update release notes by adding zip file which is found in
## com.hybris.hyeclipse.site/target/com.hybris.hyeclipse.site-1.5.0.RELEASE.zip
gren changelog -T <github-token> -G --tags=all --override
# create pull request on github.com from your private repository
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=1.5.1-SNAPSHOT
git add */*/MANIFEST.MF */pom.xml */feature.xml pom.xml CHANGELOG.md

```