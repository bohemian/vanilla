# vanilla
A springboot starter project with the following:
- Gradle wrapper build
- Spock testing framework
- Flyway for database migrations
- Docker for local development
- Lombok to eradicate boilerplate code
- Jacoco for test coverage
- Make for common commands

# Setup

## Install docker
```aiignore
brew uninstall --cask docker --force
brew cleanup
brew install --cask docker
```

## Install postgres
```aiignore
brew install postgresql
psql -U $(whoami) -d postgres -c "create user user1 with superuser password 'password1'"
```

## Install java
```aiignore
xcode-select --install
echo 'export JAVA_HOME=$(/usr/libexec/java_home)' >> ~/.zprofile
```

## Install tools
```aiignore
brew install --cask postman
```

# How to run

- ./gradlew build
- `java -jar $(find . -name "vanilla-*.jar") --spring.profiles.active=local`

## Banner
Use a site like https://patorjk.com/software/taag/#p=display&f=Big&t=Vanilla

