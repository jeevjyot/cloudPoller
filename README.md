## Cloud Poller 

This application is written in Java using [reactor](https://projectreactor.io/)  framework with gradle as a build tool.

The basic flow is 

```shell
Starts the pipeline -> polls the event -> Process the event -> emit the event (to standard outout)
```
### To build and Test

1. Make sure Java 11 is installed or install using
```shell
   brew cask install java
```
2. Build using 
```shell
   git clone https://github.com/jeevjyot/cloudPoller.git
   cd repo //to the root director
   ./gradlew clean build -x test
```
3. Testing, make sure you're exporting AWS secret and access key
```shell
export AWS_ACCESS_KEY_ID={replace with your access key}
export AWS_SECRET_ACCESS_KEY={replace with your }
./gradlew clean test
```


### To Run the application 

```shell
./gradlew run
```
