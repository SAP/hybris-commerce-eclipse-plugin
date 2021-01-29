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


## Incrementing project version

It is required to change project version before releasing latest changes. To do that it is possible to use one of Tycho functionalities for that 

```bash
# create release version
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=1.5.0.RELEASE
# add all changes to git and push to your repository (i.e. feature.xml, MANIFEST.MF and pom.xml)
mvn clean install
rm -rf updatesite/neon && cp -R com.hybris.hyeclipse.site/target/repository updatesite/neon
# push all changes to your git repository
git add updatesite/neon/* updatesite/neon/*/* */*/MANIFEST.MF */pom.xml */feature.xml pom.xml
# git add <changed files>
git push origin master
# create pull request on github.com from your private repository
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=1.5.1-SNAPSHOT
git add . */*/MANIFEST.MF */pom.xml */feature.xml pom.xml
```